package com.teammatch.backend.service;

import com.teammatch.backend.dto.StudentSkillAssignmentRequest;
import com.teammatch.backend.dto.StudentSkillNameAssignmentRequest;
import com.teammatch.backend.dto.StudentSkillResponse;
import com.teammatch.backend.entity.Skill;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.StudentSkill;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.SkillRepository;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.repository.StudentSkillRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentSkillService {
	private final StudentRepository studentRepository;
	private final SkillRepository skillRepository;
	private final StudentSkillRepository studentSkillRepository;

	public StudentSkillService(StudentRepository studentRepository, SkillRepository skillRepository,
			StudentSkillRepository studentSkillRepository) {
		this.studentRepository = studentRepository;
		this.skillRepository = skillRepository;
		this.studentSkillRepository = studentSkillRepository;
	}

	public StudentSkillResponse assign(Long studentId, StudentSkillAssignmentRequest request) {
		Student student = studentRepository.findById(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student " + studentId + " was not found."));
		Skill skill = skillRepository.findById(request.skillId())
				.orElseThrow(() -> new ResourceNotFoundException("Skill " + request.skillId() + " was not found."));

		if (studentSkillRepository.existsByStudent_IdAndSkill_Id(studentId, skill.getId())) {
			throw new DuplicateResourceException("This skill is already assigned to the student.");
		}

		StudentSkill studentSkill = new StudentSkill(student, skill, request.proficiencyLevel());
		return toResponse(studentSkillRepository.save(studentSkill));
	}

	public StudentSkillResponse assignByName(Long studentId, StudentSkillNameAssignmentRequest request) {
		Student student = studentRepository.findById(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student " + studentId + " was not found."));
		String skillName = request.skillName().trim();
		Skill skill = skillRepository.findFirstByNameIgnoreCaseOrderByIdAsc(skillName)
				.orElseGet(() -> skillRepository.save(new Skill(skillName)));

		if (studentSkillRepository.existsByStudent_IdAndSkill_Id(studentId, skill.getId())) {
			throw new DuplicateResourceException("This skill is already assigned to the student.");
		}

		StudentSkill studentSkill = new StudentSkill(student, skill, request.proficiencyLevel());
		return toResponse(studentSkillRepository.save(studentSkill));
	}

	@Transactional(readOnly = true)
	public List<StudentSkillResponse> findByStudentId(Long studentId) {
		if (!studentRepository.existsById(studentId)) {
			throw new ResourceNotFoundException("Student " + studentId + " was not found.");
		}
		return studentSkillRepository.findAllByStudent_Id(studentId).stream()
				.map(this::toResponse)
				.toList();
	}

	private StudentSkillResponse toResponse(StudentSkill studentSkill) {
		return new StudentSkillResponse(studentSkill.getStudent().getId(), studentSkill.getSkill().getId(),
				studentSkill.getSkill().getName(), studentSkill.getProficiencyLevel());
	}
}