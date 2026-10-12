package com.teammatch.backend.controller;

import com.teammatch.backend.dto.StudentCreateRequest;
import com.teammatch.backend.dto.StudentResponse;
import com.teammatch.backend.dto.StudentUpdateRequest;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.StudentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {
	private final StudentService studentService;

	public StudentController(StudentService studentService) {
		this.studentService = studentService;
	}

	@GetMapping
	public List<StudentResponse> findAll() {
		return studentService.findAll();
	}

	@GetMapping("/{id}")
	public StudentResponse findById(@PathVariable Long id,
			@AuthenticationPrincipal TeamMatchPrincipal principal) {
		requireStudentAccess(id, principal);
		return studentService.findById(id);
	}

	@PostMapping
	public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentCreateRequest request) {
		StudentResponse response = studentService.create(request);
		return ResponseEntity.created(URI.create("/api/students/" + response.id())).body(response);
	}

	@PutMapping("/{id}")
	public StudentResponse update(@PathVariable Long id, @AuthenticationPrincipal TeamMatchPrincipal principal,
			@Valid @RequestBody StudentUpdateRequest request) {
		requireStudentAccess(id, principal);
		return studentService.update(id, request);
	}

	private void requireStudentAccess(Long studentId, TeamMatchPrincipal principal) {
		if (principal == null || principal.getRole() != AccountRole.STUDENT
				|| principal.getStudentId() == null || !principal.getStudentId().equals(studentId)) {
			throw new ForbiddenOperationException("You can only manage your own student profile.");
		}
	}
}