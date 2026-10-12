package com.teammatch.backend.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.security.TeamMatchPrincipal;
import java.util.Optional;

class DevSecurityConfigurationTest {
	@Test
	void developmentUserUsesOneEncodedPasswordAndUserRole() {
		PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
		StudentRepository studentRepository = org.mockito.Mockito.mock(StudentRepository.class);
		when(passwordEncoder.encode("test-only-development-password")).thenReturn("encoded-test-password");

		UserDetailsService userDetailsService = new DevSecurityConfiguration().devUserDetailsService(
				"test-user", "test-only-development-password", passwordEncoder, studentRepository,
				new ConfiguredAdminAccount(null, null));
		UserDetails user = userDetailsService.loadUserByUsername("test-user");

		assertEquals("encoded-test-password", user.getPassword());
		assertNotEquals("test-only-development-password", user.getPassword());
		assertTrue(user.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
		verify(passwordEncoder, times(1)).encode("test-only-development-password");
		verify(studentRepository).findByEmail("test-user");
	}

	@Test
	void developmentProfileAlsoAuthenticatesDatabaseStudentAccounts() {
		PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
		StudentRepository studentRepository = org.mockito.Mockito.mock(StudentRepository.class);
		Student student = org.mockito.Mockito.mock(Student.class);
		when(passwordEncoder.encode("test-only-development-password")).thenReturn("encoded-test-password");
		when(studentRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));
		when(student.getId()).thenReturn(42L);
		when(student.getEmail()).thenReturn("student@example.com");
		when(student.getPasswordHash()).thenReturn("student-password-hash");
		when(student.getAccountRole()).thenReturn(AccountRole.STUDENT);

		UserDetailsService userDetailsService = new DevSecurityConfiguration().devUserDetailsService(
				"test-user", "test-only-development-password", passwordEncoder, studentRepository,
				new ConfiguredAdminAccount(null, null));
		TeamMatchPrincipal principal = (TeamMatchPrincipal) userDetailsService
				.loadUserByUsername("STUDENT@EXAMPLE.COM");

		assertEquals(42L, principal.getStudentId());
		assertEquals("student-password-hash", principal.getPassword());
		assertEquals(AccountRole.STUDENT, principal.getRole());
	}

	@Test
	void developmentAndAdministratorAccountsCannotShareAUsername() {
		PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
		StudentRepository studentRepository = org.mockito.Mockito.mock(StudentRepository.class);

		assertThrows(IllegalStateException.class, () -> new DevSecurityConfiguration().devUserDetailsService(
				"admin@example.com", "development-password", passwordEncoder, studentRepository,
				new ConfiguredAdminAccount("admin@example.com", "encoded-admin-password")));
	}

	@Test
	void configurationIsLimitedToDevelopmentProfile() {
		Profile profile = DevSecurityConfiguration.class.getAnnotation(Profile.class);

		assertArrayEquals(new String[] { "dev" }, profile.value());
	}
}