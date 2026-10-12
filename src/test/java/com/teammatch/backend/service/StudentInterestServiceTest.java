package com.teammatch.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teammatch.backend.dto.StudentInterestsRequest;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.StudentRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentInterestServiceTest {
	@Mock
	private StudentRepository studentRepository;

	@InjectMocks
	private StudentInterestService studentInterestService;

	@Test
	void returnsSavedInterestsForStudent() {
		Student student = studentWithInterests("Robotics", "Music");
		when(studentRepository.findById(5L)).thenReturn(Optional.of(student));

		assertEquals(List.of("Robotics", "Music"), studentInterestService.findByStudentId(5L).interests());
	}

	@Test
	void replacesInterestsAfterTrimmingAndNormalizingDuplicates() {
		Student student = studentWithInterests("Old interest");
		when(studentRepository.findById(5L)).thenReturn(Optional.of(student));
		when(studentRepository.save(student)).thenReturn(student);

		var response = studentInterestService.replace(5L,
				new StudentInterestsRequest(List.of(" Robotics ", "Music")));

		assertEquals(List.of("Robotics", "Music"), response.interests());
		assertEquals(new LinkedHashSet<>(List.of("Robotics", "Music")), student.getInterests());
	}

	@Test
	void rejectsDuplicateInterestsIgnoringCaseWithoutSaving() {
		Student student = studentWithInterests();
		when(studentRepository.findById(5L)).thenReturn(Optional.of(student));

		assertThrows(DuplicateResourceException.class,
				() -> studentInterestService.replace(5L, new StudentInterestsRequest(List.of("Music", " music "))));

		verify(studentRepository, never()).save(any(Student.class));
	}

	@Test
	void addsMissingInterestsWithoutChangingExistingValues() {
		Student student = studentWithInterests(" Robotics ");
		when(studentRepository.findById(5L)).thenReturn(Optional.of(student));
		when(studentRepository.save(student)).thenReturn(student);

		var response = studentInterestService.addMissing(5L, List.of("Robotics", " Music "));

		assertEquals(List.of(" Robotics ", "Music"), response.interests());
		assertEquals(new LinkedHashSet<>(List.of(" Robotics ", "Music")), student.getInterests());
	}

	@Test
	void rejectsCaseVariantExistingInterestsWithoutChangingOrSavingThem() {
		Student student = studentWithInterests("Robotics", "robotics");
		when(studentRepository.findById(5L)).thenReturn(Optional.of(student));

		assertThrows(DuplicateResourceException.class,
				() -> studentInterestService.addMissing(5L, List.of("Music")));

		assertEquals(new LinkedHashSet<>(List.of("Robotics", "robotics")), student.getInterests());
		verify(studentRepository, never()).save(any(Student.class));
	}

	@Test
	void rejectsMissingStudent() {
		when(studentRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> studentInterestService.findByStudentId(99L));
	}

	private Student studentWithInterests(String... interests) {
		Student student = new Student("Student", "ROLL001", "student@example.com", "encoded-password-hash", "CSE", 2);
		student.setInterests(new LinkedHashSet<>(List.of(interests)));
		return student;
	}
}
