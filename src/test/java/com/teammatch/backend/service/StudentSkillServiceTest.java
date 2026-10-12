package com.teammatch.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teammatch.backend.dto.StudentSkillAssignmentRequest;
import com.teammatch.backend.dto.StudentSkillNameAssignmentRequest;
import com.teammatch.backend.dto.StudentSkillResponse;
import com.teammatch.backend.entity.Skill;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.StudentSkill;
import com.teammatch.backend.entity.enums.SkillProficiency;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.SkillRepository;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.StudentSkillRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentSkillServiceTest {
	@Mock
	private StudentRepository studentRepository;

	@Mock
	private SkillRepository skillRepository;

	@Mock
	private StudentSkillRepository studentSkillRepository;

	@InjectMocks
	private StudentSkillService studentSkillService;

	@Test
	void assignStoresSkillAndOptionalProficiency() {
		Student student = mock(Student.class);
		Skill skill = mock(Skill.class);
		when(student.getId()).thenReturn(1L);
		when(skill.getId()).thenReturn(2L);
		when(skill.getName()).thenReturn("Java");
		when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
		when(skillRepository.findById(2L)).thenReturn(Optional.of(skill));
		when(studentSkillRepository.existsByStudent_IdAndSkill_Id(1L, 2L)).thenReturn(false);
		when(studentSkillRepository.save(any(StudentSkill.class))).thenAnswer(invocation -> invocation.getArgument(0));

		StudentSkillResponse response = studentSkillService.assign(1L,
				new StudentSkillAssignmentRequest(2L, SkillProficiency.ADVANCED));

		ArgumentCaptor<StudentSkill> assignmentCaptor = ArgumentCaptor.forClass(StudentSkill.class);
		verify(studentSkillRepository).save(assignmentCaptor.capture());
		assertEquals(student, assignmentCaptor.getValue().getStudent());
		assertEquals(skill, assignmentCaptor.getValue().getSkill());
		assertEquals(SkillProficiency.ADVANCED, response.proficiencyLevel());
		assertEquals("Java", response.skillName());
	}

	@Test
	void listReturnsAssignedSkillsForStudent() {
		Student student = mock(Student.class);
		Skill skill = mock(Skill.class);
		StudentSkill assignment = mock(StudentSkill.class);
		when(student.getId()).thenReturn(1L);
		when(skill.getId()).thenReturn(2L);
		when(skill.getName()).thenReturn("Java");
		when(assignment.getStudent()).thenReturn(student);
		when(assignment.getSkill()).thenReturn(skill);
		when(assignment.getProficiencyLevel()).thenReturn(SkillProficiency.INTERMEDIATE);
		when(studentRepository.existsById(1L)).thenReturn(true);
		when(studentSkillRepository.findAllByStudent_Id(1L)).thenReturn(List.of(assignment));

		List<StudentSkillResponse> response = studentSkillService.findByStudentId(1L);

		assertEquals(1, response.size());
		assertEquals("Java", response.get(0).skillName());
		assertEquals(SkillProficiency.INTERMEDIATE, response.get(0).proficiencyLevel());
	}

	@Test
	void assignRejectsMissingStudent() {
		when(studentRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> studentSkillService.assign(99L,
				new StudentSkillAssignmentRequest(2L, null)));
		verify(studentSkillRepository, never()).save(any(StudentSkill.class));
	}

	@Test
	void assignRejectsMissingSkill() {
		Student student = mock(Student.class);
		when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
		when(skillRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> studentSkillService.assign(1L,
				new StudentSkillAssignmentRequest(99L, null)));
		verify(studentSkillRepository, never()).save(any(StudentSkill.class));
	}

	@Test
	void assignRejectsDuplicateStudentSkillPair() {
		Student student = mock(Student.class);
		Skill skill = mock(Skill.class);
		when(skill.getId()).thenReturn(2L);
		when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
		when(skillRepository.findById(2L)).thenReturn(Optional.of(skill));
		when(studentSkillRepository.existsByStudent_IdAndSkill_Id(1L, 2L)).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> studentSkillService.assign(1L,
				new StudentSkillAssignmentRequest(2L, null)));
		verify(studentSkillRepository, never()).save(any(StudentSkill.class));
	}

	@Test
	void assignByNameTrimsNameAndCreatesCatalogSkillWhenMissing() {
		Student student = mock(Student.class);
		Skill skill = mock(Skill.class);
		when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
		when(skillRepository.findFirstByNameIgnoreCaseOrderByIdAsc("Kotlin")).thenReturn(Optional.empty());
		when(skillRepository.save(any(Skill.class))).thenReturn(skill);
		when(skill.getId()).thenReturn(3L);
		when(skill.getName()).thenReturn("Kotlin");
		when(studentSkillRepository.existsByStudent_IdAndSkill_Id(1L, 3L)).thenReturn(false);
		when(studentSkillRepository.save(any(StudentSkill.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StudentSkillResponse response = studentSkillService.assignByName(1L,
				new StudentSkillNameAssignmentRequest("  Kotlin  ", SkillProficiency.BEGINNER));

		ArgumentCaptor<Skill> skillCaptor = ArgumentCaptor.forClass(Skill.class);
		verify(skillRepository).save(skillCaptor.capture());
		assertEquals("Kotlin", skillCaptor.getValue().getName());
		assertEquals(3L, response.skillId());
		assertEquals("Kotlin", response.skillName());
		assertEquals(SkillProficiency.BEGINNER, response.proficiencyLevel());
	}

	@Test
	void assignByNameReusesCaseInsensitiveCatalogSkill() {
		Student student = mock(Student.class);
		Skill skill = mock(Skill.class);
		when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
		when(skillRepository.findFirstByNameIgnoreCaseOrderByIdAsc("java"))
				.thenReturn(Optional.of(skill));
		when(skill.getId()).thenReturn(2L);
		when(skill.getName()).thenReturn("Java");
		when(studentSkillRepository.existsByStudent_IdAndSkill_Id(1L, 2L)).thenReturn(false);
		when(studentSkillRepository.save(any(StudentSkill.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StudentSkillResponse response = studentSkillService.assignByName(1L,
				new StudentSkillNameAssignmentRequest(" java ", null));

		verify(skillRepository, never()).save(any(Skill.class));
		assertEquals("Java", response.skillName());
	}

	@Test
	void assignByNameRejectsAlreadyAssignedSkill() {
		Student student = mock(Student.class);
		Skill skill = mock(Skill.class);
		when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
		when(skillRepository.findFirstByNameIgnoreCaseOrderByIdAsc("Java"))
				.thenReturn(Optional.of(skill));
		when(skill.getId()).thenReturn(2L);
		when(studentSkillRepository.existsByStudent_IdAndSkill_Id(1L, 2L)).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> studentSkillService.assignByName(1L,
				new StudentSkillNameAssignmentRequest("Java", null)));
		verify(studentSkillRepository, never()).save(any(StudentSkill.class));
	}

	@Test
	void listRejectsMissingStudent() {
		when(studentRepository.existsById(99L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> studentSkillService.findByStudentId(99L));
		verify(studentSkillRepository, never()).findAllByStudent_Id(99L);
	}
}