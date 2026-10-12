package com.teammatch.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.security.TeamMatchPrincipal;
import java.util.Locale;

@Configuration(proxyBeanMethods = false)
@Profile("dev")
@Import(AdminAccountConfiguration.class)
public class DevSecurityConfiguration {
	@Bean
	UserDetailsService devUserDetailsService(
			@Value("${TEAMMATCH_DEV_USERNAME}") String username,
			@Value("${TEAMMATCH_DEV_PASSWORD}") String password,
			PasswordEncoder passwordEncoder,
			StudentRepository studentRepository,
			ConfiguredAdminAccount configuredAdminAccount) {
		if (configuredAdminAccount.findPrincipal(username).isPresent()) {
			throw new IllegalStateException("The configured development and administrator usernames must differ.");
		}
		String encodedPassword = passwordEncoder.encode(password);
		UserDetails user = new TeamMatchPrincipal(username, encodedPassword);
		UserDetailsService developmentUserDetails = new InMemoryUserDetailsManager(user);
		return requestedUsername -> {
			String email = requestedUsername.trim().toLowerCase(Locale.ROOT);
			var admin = configuredAdminAccount.findPrincipal(email);
			if (admin.isPresent()) {
				return admin.get();
			}
			var student = studentRepository.findByEmail(email);
			if (student.isPresent()) {
				return new TeamMatchPrincipal(student.get());
			}
			if (username.equals(requestedUsername)) {
				return developmentUserDetails.loadUserByUsername(requestedUsername);
			}
			throw new UsernameNotFoundException("Student account was not found.");
		};
	}
}