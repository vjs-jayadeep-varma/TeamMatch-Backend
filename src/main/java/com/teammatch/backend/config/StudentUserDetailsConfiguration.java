package com.teammatch.backend.config;

import com.teammatch.backend.entity.Student;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.security.TeamMatchPrincipal;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration(proxyBeanMethods = false)
@Profile("!dev")
public class StudentUserDetailsConfiguration {
	@Bean
	UserDetailsService studentUserDetailsService(
			StudentRepository studentRepository, ConfiguredAdminAccount configuredAdminAccount) {
		return username -> {
			String email = username.trim().toLowerCase(Locale.ROOT);
			var admin = configuredAdminAccount.findPrincipal(email);
			if (admin.isPresent()) {
				return admin.get();
			}
			Student student = studentRepository.findByEmail(email)
					.orElseThrow(() -> new UsernameNotFoundException("Student account was not found."));
			return new TeamMatchPrincipal(student);
		};
	}
}
