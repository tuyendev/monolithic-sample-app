package io.github.tuyendev.mbs.common.service.auth;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthorizationRequestDto implements Serializable {

	@NotNull(message = "{TODO-ADD-MESSAGE}")
	private String username;

	@NotNull(message = "{TODO-ADD-MESSAGE}")
	private String password;

	private boolean rememberMe;
}