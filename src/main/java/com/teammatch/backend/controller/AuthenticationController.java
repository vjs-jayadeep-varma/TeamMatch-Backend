package com.teammatch.backend.controller;

import com.teammatch.backend.dto.StudentLoginRequest;
import com.teammatch.backend.dto.StudentResponse;
import com.teammatch.backend.dto.AuthenticatedUserResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.exception.AuthenticationFailedException;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final StudentService studentService;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy =
			new ChangeSessionIdAuthenticationStrategy();
	private final CsrfAuthenticationStrategy csrfAuthenticationStrategy =
			new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository());
	private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

	public AuthenticationController(AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository, StudentService studentService) {
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.studentService = studentService;
	}

	@PostMapping("/login")
	public AuthenticatedUserResponse login(@Valid @RequestBody StudentLoginRequest request,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim(), request.password()));
		} catch (AuthenticationException exception) {
			throw new AuthenticationFailedException("Email or password is incorrect.");
		}

		if (!(authentication.getPrincipal() instanceof TeamMatchPrincipal principal)
				|| principal.getRole() == null
				|| (principal.getRole() == AccountRole.STUDENT && principal.getStudentId() == null)
				|| (principal.getRole() != AccountRole.STUDENT && principal.getRole() != AccountRole.ADMIN)) {
			throw new AuthenticationFailedException("A registered student or configured administrator account is required.");
		}

		sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);
		csrfAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, httpRequest, httpResponse);
		return authenticatedUser(principal);
	}

	@GetMapping("/me")
	public AuthenticatedUserResponse currentUser(@AuthenticationPrincipal TeamMatchPrincipal principal) {
		if (principal == null || principal.getRole() == null
				|| (principal.getRole() == AccountRole.STUDENT && principal.getStudentId() == null)
				|| (principal.getRole() != AccountRole.STUDENT && principal.getRole() != AccountRole.ADMIN)) {
			throw new AuthenticationFailedException("A registered student or configured administrator account is required.");
		}
		return authenticatedUser(principal);
	}

	private AuthenticatedUserResponse authenticatedUser(TeamMatchPrincipal principal) {
		if (principal.getRole() == AccountRole.ADMIN) {
			return AuthenticatedUserResponse.admin(principal.getUsername());
		}
		StudentResponse student = studentService.findById(principal.getStudentId());
		return AuthenticatedUserResponse.fromStudent(student);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) {
		logoutHandler.logout(request, response, authentication);
		SecurityContextHolder.clearContext();
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
