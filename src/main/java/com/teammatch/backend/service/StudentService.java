package com.teammatch.backend.service;

import com.teammatch.backend.dto.StudentCreateRequest;
import com.teammatch.backend.dto.StudentResponse;
import com.teammatch.backend.dto.StudentUpdateRequest;
import com.teammatch.backend.config.ConfiguredAdminAccount;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.StudentRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentService {
	private final StudentRepository studentRepository;
	private final PasswordEncoder passwordEncoder;
	private final ConfiguredAdminAccount configuredAdminAccount;

	public StudentService(StudentRepository studentRepository, PasswordEncoder passwordEncoder,
			ConfiguredAdminAccount configuredAdminAccount) {
		this.studentRepository = studentRepository;
		this.passwordEncoder = passwordEncoder;
		this.configuredAdminAccount = configuredAdminAccount;
	}

	@Transactional(readOnly = true)
	public List<StudentResponse> findAll() {
		return studentRepository.findAll().stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public StudentResponse findById(Long id) {
		return toResponse(getStudent(id));
	}

	public StudentResponse create(StudentCreateRequest request) {
		String email = normalizeEmail(request.getEmail());
		String rollNumber = request.getRollNumber().trim();
		ensureUnique(email, rollNumber, null);

		Student student = new Student(request.getName().trim(), rollNumber, email,
				passwordEncoder.encode(request.getPassword()), request.getDepartment().trim(), request.getAcademicYear());
		student.setAccountRole(AccountRole.STUDENT);
		student.setAvailable(true);
		return toResponse(studentRepository.save(student));
	}

	public StudentResponse update(Long id, StudentUpdateRequest request) {
		Student student = getStudent(id);
		String email = normalizeEmail(request.getEmail());
		String rollNumber = request.getRollNumber().trim();
		ensureUnique(email, rollNumber, id);

		student.setName(request.getName().trim());
		student.setRollNumber(rollNumber);
		student.setEmail(email);
		student.setDepartment(request.getDepartment().trim());
		student.setAcademicYear(request.getAcademicYear());
		student.setAvailable(request.getAvailable());
		if (request.getPassword() != null) {
			student.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		}
		return toResponse(studentRepository.save(student));
	}

	private Student getStudent(Long id) {
		return studentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Student " + id + " was not found."));
	}

	private void ensureUnique(String email, String rollNumber, Long excludedId) {
		if (configuredAdminAccount.matchesUsername(email)) {
			throw new DuplicateResourceException("This email is reserved for the configured administrator account.");
		}
		boolean emailExists = excludedId == null
				? studentRepository.existsByEmail(email)
				: studentRepository.existsByEmailAndIdNot(email, excludedId);
		if (emailExists) {
			throw new DuplicateResourceException("A student with this email already exists.");
		}

		boolean rollNumberExists = excludedId == null
				? studentRepository.existsByRollNumber(rollNumber)
				: studentRepository.existsByRollNumberAndIdNot(rollNumber, excludedId);
		if (rollNumberExists) {
			throw new DuplicateResourceException("A student with this roll number already exists.");
		}
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private StudentResponse toResponse(Student student) {
		return new StudentResponse(student.getId(), student.getName(), student.getRollNumber(), student.getEmail(),
				student.getDepartment(), student.getAcademicYear(), student.isAvailable(), student.getAccountRole());
	}
}