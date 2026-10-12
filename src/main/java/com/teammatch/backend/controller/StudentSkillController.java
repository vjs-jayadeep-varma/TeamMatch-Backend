package com.teammatch.backend.controller;

import com.teammatch.backend.dto.StudentSkillAssignmentRequest;
import com.teammatch.backend.dto.StudentSkillNameAssignmentRequest;
import com.teammatch.backend.dto.StudentSkillResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.StudentSkillService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/{studentId}/skills")
public class StudentSkillController {
	private final StudentSkillService studentSkillService;

	public StudentSkillController(StudentSkillService studentSkillService) {
		this.studentSkillService = studentSkillService;
	}

	@GetMapping
	public List<StudentSkillResponse> findByStudentId(@PathVariable Long studentId,
			@AuthenticationPrincipal TeamMatchPrincipal principal) {
		requireStudentAccess(studentId, principal);
		return studentSkillService.findByStudentId(studentId);
	}

	@PostMapping
	public ResponseEntity<StudentSkillResponse> assign(@PathVariable Long studentId,
			@AuthenticationPrincipal TeamMatchPrincipal principal,
			@Valid @RequestBody StudentSkillAssignmentRequest request) {
		requireStudentAccess(studentId, principal);
		StudentSkillResponse response = studentSkillService.assign(studentId, request);
		URI location = URI.create("/api/students/" + studentId + "/skills/" + response.skillId());
		return ResponseEntity.created(location).body(response);
	}

	@PostMapping("/by-name")
	public ResponseEntity<StudentSkillResponse> assignByName(@PathVariable Long studentId,
			@AuthenticationPrincipal TeamMatchPrincipal principal,
			@Valid @RequestBody StudentSkillNameAssignmentRequest request) {
		requireStudentAccess(studentId, principal);
		StudentSkillResponse response = studentSkillService.assignByName(studentId, request);
		URI location = URI.create("/api/students/" + studentId + "/skills/" + response.skillId());
		return ResponseEntity.created(location).body(response);
	}

	private void requireStudentAccess(Long studentId, TeamMatchPrincipal principal) {
		if (principal == null || principal.getRole() != AccountRole.STUDENT
				|| principal.getStudentId() == null || !principal.getStudentId().equals(studentId)) {
			throw new ForbiddenOperationException("You can only manage skills for your own student account.");
		}
	}
}