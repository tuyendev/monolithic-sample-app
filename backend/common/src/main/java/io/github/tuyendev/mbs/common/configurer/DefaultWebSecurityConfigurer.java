package io.github.tuyendev.mbs.common.configurer;


import io.github.tuyendev.mbs.common.CommonConstants;
import io.github.tuyendev.mbs.common.security.jwt.JwtSecurityAdapter;
import io.github.tuyendev.mbs.common.security.jwt.JwtTokenProvider;
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@EnableWebSecurity(debug = true)
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@Import(SecurityProblemSupport.class)
class DefaultWebSecurityConfigurer {
	private final SecurityProblemSupport problemSupport;

	private final JwtTokenProvider tokenProvider;

	public DefaultWebSecurityConfigurer(SecurityProblemSupport problemSupport, JwtTokenProvider tokenProvider) {
		this.problemSupport = problemSupport;
		this.tokenProvider = tokenProvider;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		// @formatter:off
		http.cors()
				.and().csrf().disable()
				.exceptionHandling()
				.authenticationEntryPoint(problemSupport)
				.accessDeniedHandler(problemSupport)
				.and()
				.headers()
				.referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
				.and()
				.permissionsPolicy().policy("camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()")
				.and()
				.frameOptions()
				.sameOrigin()
				.and()
				.sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.and()
				.authorizeRequests()
				.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.antMatchers("/webjars/**", "/error/**").permitAll()
				.antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.antMatchers("/auth/token", "/auth/refresh_token", "/auth/forgot-password", "/auth/forgot-password-complete").permitAll()
				.antMatchers("/actuator/**").hasRole(CommonConstants.Role.DEFAULT_ROLE_ADMIN)
				.anyRequest().permitAll()
				.and()
				.formLogin().disable()
				.logout().disable()
				.httpBasic().disable()
				.apply(securityConfigurerAdapter());
		return http.build();
		// @formatter:on
	}

	private JwtSecurityAdapter securityConfigurerAdapter() {
		return new JwtSecurityAdapter(tokenProvider);
	}

	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOriginPattern("*");
		config.addAllowedMethod("*");
		config.addAllowedHeader("*");
		config.setAllowCredentials(true);
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}
}
