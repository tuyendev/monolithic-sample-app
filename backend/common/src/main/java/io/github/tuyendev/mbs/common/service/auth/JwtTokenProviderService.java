package io.github.tuyendev.mbs.common.service.auth;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import io.github.tuyendev.mbs.common.CommonConstants;
import io.github.tuyendev.mbs.common.entity.rdb.AccessToken;
import io.github.tuyendev.mbs.common.entity.rdb.RefreshToken;
import io.github.tuyendev.mbs.common.entity.rdb.User;
import io.github.tuyendev.mbs.common.repository.rdb.AccessTokenRepository;
import io.github.tuyendev.mbs.common.repository.rdb.RefreshTokenRepository;
import io.github.tuyendev.mbs.common.security.DomainUserDetails;
import io.github.tuyendev.mbs.common.security.jwt.JwtAccessToken;
import io.github.tuyendev.mbs.common.security.jwt.JwtTokenProvider;
import io.github.tuyendev.mbs.common.service.user.UserService;
import io.github.tuyendev.mbs.common.utils.AppContextUtils;
import io.github.tuyendev.mbs.common.utils.DateUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.PrematureJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class JwtTokenProviderService implements JwtTokenProvider {

	private final JwtParser jwtParser;

	private final Key secretKey;

	private final AuthenticationManagerBuilder authenticationManagerBuilder;

	private final UserService userService;

	private final AccessTokenRepository accessTokenRepo;

	private final RefreshTokenRepository refreshTokenRepo;

	@Value("${app.common.jwt.access-token-expiration}")
	private long accessTokenExpirationInSeconds;

	@Value("${app.common.jwt.refresh-token-expiration}")
	private long refreshTokenExpirationInSeconds;

	public JwtTokenProviderService(@Value("${app.common.jwt.secret-key}") String jwtSecretKey,
			AuthenticationManagerBuilder authenticationManagerBuilder, UserService userService,
			AccessTokenRepository accessTokenRepo, RefreshTokenRepository refreshTokenRepo) {
		this.userService = userService;
		byte[] keyBytes = Decoders.BASE64.decode(jwtSecretKey);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
		this.jwtParser = Jwts.parserBuilder().setSigningKey(secretKey).build();
		this.authenticationManagerBuilder = authenticationManagerBuilder;
		this.accessTokenRepo = accessTokenRepo;
		this.refreshTokenRepo = refreshTokenRepo;
	}


	@Override
	public JwtAccessToken generateToken(String username, String password, boolean rememberMe) {
		Authentication authenticationToken = UsernamePasswordAuthenticationToken.unauthenticated(username, password);
		Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		return createToken(rememberMe);
	}

	private JwtAccessToken createToken(final boolean rememberMe) {
		User currentUser = AppContextUtils.getCurrentLoginUser()
				.orElseThrow(() -> new RuntimeException("This should never happen since the authentication is completed before."));
		final Date issuedAt = new Date();
		final Date accessTokenExpiration = new Date(issuedAt.getTime() + accessTokenExpirationInSeconds * 1000);
		AccessToken accessToken = createAccessToken(currentUser, accessTokenExpiration, issuedAt);
		String refreshJwtToken = null;
		if (rememberMe) {
			RefreshToken refreshToken = createRefreshToken(accessToken.getId(), accessTokenExpiration, issuedAt);
			refreshJwtToken = refreshToken.getToken();
			accessToken.setRefreshToken(refreshToken);
		}
		accessTokenRepo.save(accessToken);
		return createJwtAccessToken(accessToken.getToken(), refreshJwtToken, accessToken.getExpiredAt());
	}

	private AccessToken createAccessToken(final User user, Date expiration, Date issuedAt) {
		final String id = UUID.randomUUID().toString();
		final String token = Jwts.builder()
				.setId(id)
				.setAudience(CommonConstants.TokenAudience.ACCESS_TOKEN)
				.setSubject(user.getId().toString())
				.setIssuedAt(issuedAt)
				.setNotBefore(issuedAt)
				.setExpiration(expiration)
				.signWith(secretKey)
				.compact();
		return AccessToken.builder()
				.id(id)
				.userId(user.getId())
				.token(token)
				.expiredAt(DateUtils.dateToLocalDateTime(expiration))
				.status(CommonConstants.EntityStatus.ACTIVE)
				.build();
	}

	private RefreshToken createRefreshToken(final String accessTokenId, Date notBefore, Date issuedAt) {
		Date expiration = new Date(issuedAt.getTime() + refreshTokenExpirationInSeconds * 1000);
		final String id = UUID.randomUUID().toString();
		final String token = Jwts.builder()
				.setId(id)
				.setAudience(CommonConstants.TokenAudience.REFRESH_TOKEN)
				.setSubject(accessTokenId)
				.setIssuedAt(issuedAt)
				.setNotBefore(notBefore)
				.setExpiration(expiration)
				.signWith(secretKey)
				.compact();
		return RefreshToken.builder()
				.id(id)
				.status(CommonConstants.EntityStatus.ACTIVE)
				.expiredAt(DateUtils.dateToLocalDateTime(expiration))
				.token(token)
				.build();
	}

	private JwtAccessToken createJwtAccessToken(final String accessToken, final String refreshToken, final LocalDateTime expiredAt) {
		return JwtAccessToken.builder()
				.type("Bearer")
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.expiration(DateUtils.localDateTimeToDate(expiredAt).getTime())
				.build();
	}


	@Override
	public JwtAccessToken refreshToken(String jwtToken) {
		Claims claims = getClaims(jwtToken);
		if (!Objects.equals(claims.getAudience(), CommonConstants.TokenAudience.REFRESH_TOKEN)) {
			throw new InvalidAudienceTokenException();
		}
		if (!refreshTokenRepo.existsRefreshTokenByIdAndStatus(claims.getId())) {
			throw new RevokedJwtTokenException();
		}
		AccessToken accessToken = accessTokenRepo.findActiveAccessTokenById(claims.getSubject()).
				orElseThrow(RevokedJwtTokenException::new);
		accessToken.setStatus(CommonConstants.EntityStatus.DELETED);
		accessToken.getRefreshToken().setStatus(CommonConstants.EntityStatus.DELETED);
		accessTokenRepo.save(accessToken);
		setAuthenticationByRefreshToken(accessToken.getUserId(), jwtToken);
		return createToken(true);
	}

	private Claims getClaims(String jwt) {
		try {
			return jwtParser.parseClaimsJws(jwt).getBody();
		}
		catch (ExpiredJwtException | MalformedJwtException | UnsupportedJwtException | SignatureException |
			   PrematureJwtException | IllegalArgumentException e) {
			log.trace("Invalid JWT jwt", e);
			throw new InvalidJwtTokenException();
		}
	}

	private void setAuthenticationByRefreshToken(Long userId, String jwtToken) {
		setAuthenticationAfterSuccess(userId, jwtToken);
	}

	private void setAuthenticationAfterSuccess(Long userId, String jwtToken) {
		User user = userService.findActiveUserById(userId);
		UserDetails userDetails = buildPrincipalForRefreshTokenFromUser(user, jwtToken);
		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private UserDetails buildPrincipalForRefreshTokenFromUser(final User user, final String jwt) {
		DomainUserDetails userDetails = new DomainUserDetails(user, jwt);
		new AccountStatusUserDetailsChecker().check(userDetails);
		return userDetails;
	}

	@Override
	public void authorizeToken(String jwtToken) {
		Claims claims = getClaims(jwtToken);
		if (!Objects.equals(claims.getAudience(), CommonConstants.TokenAudience.ACCESS_TOKEN)) {
			throw new InvalidAudienceTokenException();
		}
		AccessToken accessToken = accessTokenRepo.findActiveAccessTokenById(claims.getId()).
				orElseThrow(RevokedJwtTokenException::new);
		setAuthenticationByAccessToken(accessToken.getUserId(), Long.valueOf(claims.getSubject()), jwtToken);
	}

	private void setAuthenticationByAccessToken(Long userId, Long subjectUserId, String jwtToken) {
		if (!Objects.equals(subjectUserId, userId)) {
			throw new InvalidJwtTokenException();
		}
		setAuthenticationAfterSuccess(userId, jwtToken);
	}
}