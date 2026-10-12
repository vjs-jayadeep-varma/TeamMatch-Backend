package com.teammatch.backend.controller;

import com.teammatch.backend.dto.StudentTeamResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.StudentTeamService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/{studentId}/teams")
public class StudentTeamController {
	private final StudentTeamService studentTeamService;

	public StudentTeamController(StudentTeamService studentTeamService) {
		this.studentTeamService = studentTeamService;
	}

	@GetMapping
	public List<StudentTeamResponse> findTeams(@PathVariable Long studentId,
			@AuthenticationPrincipal TeamMatchPrincipal principal) {
		if (principal == null || principal.getStudentId() == null || principal.getRole() != AccountRole.STUDENT) {
			throw new ForbiddenOperationException("A student account is required to view team data.");
		}
		if (!principal.getStudentId().equals(studentId)) {
			throw new ForbiddenOperationException("You can only view teams for your own student account.");
		}
		return studentTeamService.findTeams(principal.getStudentId(), studentId);
	}
}
