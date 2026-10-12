package com.teammatch.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import com.teammatch.backend.repository.StudentRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Configuration(proxyBeanMethods = false)
public class AdminAccountConfiguration {
	@Bean
	ConfiguredAdminAccount configuredAdminAccount(
			@Value("${TEAMMATCH_ADMIN_USERNAME:}") String username,
			@Value("${TEAMMATCH_ADMIN_PASSWORD:}") String password,
			PasswordEncoder passwordEncoder,
			StudentRepository studentRepository) {
		boolean hasUsername = StringUtils.hasText(username);
		boolean hasPassword = StringUtils.hasText(password);
		if (hasUsername != hasPassword) {
			throw new IllegalStateException(
					"TEAMMATCH_ADMIN_USERNAME and TEAMMATCH_ADMIN_PASSWORD must both be configured.");
		}
		if (!hasUsername) {
			return new ConfiguredAdminAccount(null, null);
		}
		String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
		if (normalizedUsername.length() > 254) {
			throw new IllegalStateException("TEAMMATCH_ADMIN_USERNAME must be at most 254 characters.");
		}
		if (password.length() < 12 || password.length() > 72
				|| password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalStateException("TEAMMATCH_ADMIN_PASSWORD must be 12 to 72 characters and at most 72 UTF-8 bytes.");
		}
		if (studentRepository.findByEmailIgnoreCase(normalizedUsername).isPresent()) {
			throw new IllegalStateException(
					"The configured administrator username matches an existing student email; choose a different username.");
		}
		return new ConfiguredAdminAccount(
				normalizedUsername, passwordEncoder.encode(password));
	}
}
