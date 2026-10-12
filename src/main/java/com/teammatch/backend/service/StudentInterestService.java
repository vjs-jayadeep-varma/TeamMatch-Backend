package com.teammatch.backend.service;

import com.teammatch.backend.dto.StudentInterestsRequest;
import com.teammatch.backend.dto.StudentInterestsResponse;
import com.teammatch.backend.entity.Student;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.StudentRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentInterestService {
	private final StudentRepository studentRepository;

	public StudentInterestService(StudentRepository studentRepository) {
		this.studentRepository = studentRepository;
	}

	@Transactional(readOnly = true)
	public StudentInterestsResponse findByStudentId(Long studentId) {
		Student student = getStudent(studentId);
		return toResponse(student);
	}

	public StudentInterestsResponse addMissing(Long studentId, List<String> additions) {
		Student student = getStudent(studentId);
		LinkedHashSet<String> existingKeys = new LinkedHashSet<>();
		for (String interest : student.getInterests()) {
			String normalizedInterest = normalize(interest);
			if (!existingKeys.add(normalizedInterest)) {
				throw new DuplicateResourceException(
						"Existing interests contain case-insensitive duplicates; refusing to modify them.");
			}
		}

		if (student.getInterests().size() >= 20) {
			return toResponse(student);
		}

		boolean changed = false;
		for (String interest : additions) {
			String trimmedInterest = interest.trim();
			if (existingKeys.add(normalize(trimmedInterest)) && student.getInterests().size() < 20) {
				student.getInterests().add(trimmedInterest);
				changed = true;
			}
		}
		if (changed) {
			studentRepository.save(student);
		}
		return toResponse(student);
	}

	public StudentInterestsResponse replace(Long studentId, StudentInterestsRequest request) {
		Student student = getStudent(studentId);
		LinkedHashMap<String, String> normalizedInterests = new LinkedHashMap<>();
		for (String interest : request.interests()) {
			String trimmedInterest = interest.trim();
			String normalizedInterest = trimmedInterest.toLowerCase(Locale.ROOT);
			if (normalizedInterests.putIfAbsent(normalizedInterest, trimmedInterest) != null) {
				throw new DuplicateResourceException("Interests must be unique, ignoring letter case.");
			}
		}
		student.setInterests(new LinkedHashSet<>(normalizedInterests.values()));
		return toResponse(studentRepository.save(student));
	}

	private Student getStudent(Long studentId) {
		return studentRepository.findById(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student " + studentId + " was not found."));
	}

	private StudentInterestsResponse toResponse(Student student) {
		List<String> interests = List.copyOf(student.getInterests());
		return new StudentInterestsResponse(student.getId(), interests);
	}

	private String normalize(String interest) {
		return interest.trim().toLowerCase(Locale.ROOT);
	}
}
