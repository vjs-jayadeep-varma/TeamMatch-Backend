package com.teammatch.backend.controller;

import com.teammatch.backend.dto.StudentInterestsRequest;
import com.teammatch.backend.dto.StudentInterestsResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.StudentInterestService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/{studentId}/interests")
public class StudentInterestController {
	private final StudentInterestService studentInterestService;

	public StudentInterestController(StudentInterestService studentInterestService) {
		this.studentInterestService = studentInterestService;
	}

	@GetMapping
	public StudentInterestsResponse findByStudentId(@PathVariable Long studentId,
			@AuthenticationPrincipal TeamMatchPrincipal principal) {
		requireStudentAccess(studentId, principal);
		return studentInterestService.findByStudentId(studentId);
	}

	@PutMapping
	public StudentInterestsResponse replace(@PathVariable Long studentId,
			@AuthenticationPrincipal TeamMatchPrincipal principal,
			@Valid @RequestBody StudentInterestsRequest request) {
		requireStudentAccess(studentId, principal);
		return studentInterestService.replace(studentId, request);
	}

	private void requireStudentAccess(Long studentId, TeamMatchPrincipal principal) {
		if (principal == null || principal.getRole() != AccountRole.STUDENT
				|| principal.getStudentId() == null || !principal.getStudentId().equals(studentId)) {
			throw new ForbiddenOperationException("You can only manage interests for your own student account.");
		}
	}
}
