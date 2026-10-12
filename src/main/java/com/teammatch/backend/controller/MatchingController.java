package com.teammatch.backend.controller;

import com.teammatch.backend.dto.StudentRecommendationResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.MatchingService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/recommendations")
public class MatchingController {
	private final MatchingService matchingService;

	public MatchingController(MatchingService matchingService) {
		this.matchingService = matchingService;
	}

	@GetMapping
	public List<StudentRecommendationResponse> recommend(@PathVariable Long projectId,
			@AuthenticationPrincipal TeamMatchPrincipal principal) {
		if (principal == null || principal.getRole() != AccountRole.STUDENT || principal.getStudentId() == null) {
			throw new ForbiddenOperationException("A student account is required to view teammate recommendations.");
		}
		return matchingService.recommendForProject(projectId, principal.getStudentId());
	}
}