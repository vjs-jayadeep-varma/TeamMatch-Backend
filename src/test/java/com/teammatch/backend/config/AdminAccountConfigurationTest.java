package com.teammatch.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teammatch.backend.entity.Student;
import com.teammatch.backend.repository.StudentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminAccountConfigurationTest {
	@Test
	void rejectsAdminUsernameMatchingAnExistingStudentEmailIgnoringCase() {
		StudentRepository studentRepository = mock(StudentRepository.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		when(studentRepository.findByEmailIgnoreCase("admin@example.com"))
				.thenReturn(Optional.of(mock(Student.class)));

		assertThrows(IllegalStateException.class, () -> new AdminAccountConfiguration().configuredAdminAccount(
				" Admin@Example.com ", "test-only-admin-password", passwordEncoder, studentRepository));
		verify(passwordEncoder, never()).encode("test-only-admin-password");
	}

	@Test
	void normalizesConfiguredUsernameAndEncodesPassword() {
		StudentRepository studentRepository = mock(StudentRepository.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		when(studentRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("test-only-admin-password")).thenReturn("encoded-admin-password");

		ConfiguredAdminAccount account = new AdminAccountConfiguration().configuredAdminAccount(
				" Admin@Example.com ", "test-only-admin-password", passwordEncoder, studentRepository);

		assertEquals("admin@example.com", account.findPrincipal("ADMIN@example.com")
				.orElseThrow().getUsername());
		assertEquals("encoded-admin-password", account.findPrincipal("admin@example.com")
				.orElseThrow().getPassword());
	}

	@Test
	void rejectsConfiguredCredentialsOutsideLoginLimits() {
		StudentRepository studentRepository = mock(StudentRepository.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

		assertThrows(IllegalStateException.class, () -> new AdminAccountConfiguration().configuredAdminAccount(
				"a".repeat(255), "test-only-admin-password", passwordEncoder, studentRepository));
		assertThrows(IllegalStateException.class, () -> new AdminAccountConfiguration().configuredAdminAccount(
				"admin@example.com", "short", passwordEncoder, studentRepository));
		assertThrows(IllegalStateException.class, () -> new AdminAccountConfiguration().configuredAdminAccount(
				"admin@example.com", "a".repeat(73), passwordEncoder, studentRepository));
	}
}
