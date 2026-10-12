package com.teammatch.backend.controller;

import com.teammatch.backend.dto.TeamRequestCreateRequest;
import com.teammatch.backend.dto.TeamRequestDecisionRequest;
import com.teammatch.backend.dto.TeamRequestResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.TeamRequestService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TeamRequestController {
	private final TeamRequestService teamRequestService;

	public TeamRequestController(TeamRequestService teamRequestService) {
		this.teamRequestService = teamRequestService;
	}

	@PostMapping("/projects/{projectId}/team-requests")
	public ResponseEntity<TeamRequestResponse> create(@PathVariable Long projectId,
			@AuthenticationPrincipal TeamMatchPrincipal principal,
			@Valid @RequestBody TeamRequestCreateRequest request) {
		TeamRequestResponse response = teamRequestService.create(requireStudent(principal), projectId, request);
		return ResponseEntity.created(URI.create("/api/team-requests/" + response.id())).body(response);
	}

	@GetMapping("/students/{studentId}/team-requests/incoming")
	public List<TeamRequestResponse> findIncoming(@PathVariable Long studentId,
			@AuthenticationPrincipal TeamMatchPrincipal principal) {
		return teamRequestService.findIncoming(requireStudent(principal), studentId);
	}

	@GetMapping("/students/{studentId}/team-requests/incoming/history")
	public List<TeamRequestResponse> findIncomingHistory(@PathVariable Long studentId,
			@AuthenticationPrincipal TeamMatchPrincipal principal) {
		return teamRequestService.findIncomingHistory(requireStudent(principal), studentId);
	}

	@GetMapping("/students/{studentId}/team-requests/outgoing")
	public List<TeamRequestResponse> findOutgoing(@PathVariable Long studentId,
			@AuthenticationPrincipal TeamMatchPrincipal principal) {
		return teamRequestService.findOutgoing(requireStudent(principal), studentId);
	}

	@GetMapping("/students/{studentId}/team-requests/outgoing/history")
	public List<TeamRequestResponse> findOutgoingHistory(@PathVariable Long studentId,
			@AuthenticationPrincipal TeamMatchPrincipal principal) {
		return teamRequestService.findOutgoingHistory(requireStudent(principal), studentId);
	}

	@PatchMapping("/students/{studentId}/team-requests/{requestId}/response")
	public TeamRequestResponse respond(@PathVariable Long studentId, @PathVariable Long requestId,
			@AuthenticationPrincipal TeamMatchPrincipal principal,
			@Valid @RequestBody TeamRequestDecisionRequest decision) {
		return teamRequestService.respond(requireStudent(principal), studentId, requestId, decision);
	}

	private Long requireStudent(TeamMatchPrincipal principal) {
		if (principal == null || principal.getStudentId() == null || principal.getRole() != AccountRole.STUDENT) {
			throw new ForbiddenOperationException("A student account is required for team request actions.");
		}
		return principal.getStudentId();
	}
}
