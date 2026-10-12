package com.teammatch.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.security.TeamMatchPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;

class StudentUserDetailsConfigurationTest {
	@Test
	void loadsStudentPrincipalByNormalizedEmail() {
		StudentRepository studentRepository = mock(StudentRepository.class);
		Student student = mock(Student.class);
		when(studentRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));
		when(student.getId()).thenReturn(42L);
		when(student.getEmail()).thenReturn("student@example.com");
		when(student.getPasswordHash()).thenReturn("bcrypt-hash");
		when(student.getAccountRole()).thenReturn(AccountRole.STUDENT);

		UserDetailsService service = new StudentUserDetailsConfiguration()
				.studentUserDetailsService(studentRepository, new ConfiguredAdminAccount(null, null));

		TeamMatchPrincipal principal = (TeamMatchPrincipal) service.loadUserByUsername("  STUDENT@EXAMPLE.COM ");
		assertEquals(42L, principal.getStudentId());
		assertEquals("student@example.com", principal.getUsername());
		assertEquals("bcrypt-hash", principal.getPassword());
		assertEquals(AccountRole.STUDENT, principal.getRole());
	}
}
