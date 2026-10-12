package com.teammatch.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teammatch.backend.dto.StudentCreateRequest;
import com.teammatch.backend.dto.StudentResponse;
import com.teammatch.backend.config.ConfiguredAdminAccount;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
	@Mock
	private StudentRepository studentRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ConfiguredAdminAccount configuredAdminAccount;

	@InjectMocks
	private StudentService studentService;

	@Test
	void createHashesPasswordAndForcesStudentRole() {
		StudentCreateRequest request = validRequest();
		when(passwordEncoder.encode("a-secure-password")).thenReturn("encoded-password-hash");
		when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

		StudentResponse response = studentService.create(request);

		ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
		verify(studentRepository).save(studentCaptor.capture());
		Student savedStudent = studentCaptor.getValue();
		assertEquals("encoded-password-hash", savedStudent.getPasswordHash());
		assertEquals(AccountRole.STUDENT, savedStudent.getAccountRole());
		assertEquals("student1@example.com", savedStudent.getEmail());
		assertFalse(response.toString().contains("encoded-password-hash"));
		assertFalse(response.toString().contains("a-secure-password"));
	}

	@Test
	void createRejectsDuplicateEmailBeforeSaving() {
		when(studentRepository.existsByEmail("student1@example.com")).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> studentService.create(validRequest()));
		verify(studentRepository, never()).save(any(Student.class));
	}

	@Test
	void createRejectsConfiguredAdministratorUsername() {
		when(configuredAdminAccount.matchesUsername("student1@example.com")).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> studentService.create(validRequest()));
		verify(studentRepository, never()).save(any(Student.class));
	}

	private StudentCreateRequest validRequest() {
		StudentCreateRequest request = new StudentCreateRequest();
		request.setName("Student1");
		request.setRollNumber("ROLL001");
		request.setEmail(" Student1@Example.com ");
		request.setPassword("a-secure-password");
		request.setDepartment("CSE");
		request.setAcademicYear(2);
		return request;
	}
}