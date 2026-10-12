package com.teammatch.backend.controller;

import com.teammatch.backend.dto.ProjectCreateRequest;
import com.teammatch.backend.dto.ProjectResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.exception.ForbiddenOperationException;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.ProjectService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	public List<ProjectResponse> findAll() {
		return projectService.findAll();
	}

	@GetMapping("/{id}")
	public ProjectResponse findById(@PathVariable Long id) {
		return projectService.findById(id);
	}

	@PostMapping
	public ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal TeamMatchPrincipal principal,
			@Valid @RequestBody ProjectCreateRequest request) {
		if (principal == null || principal.getRole() != AccountRole.STUDENT
				|| principal.getStudentId() == null || !principal.getStudentId().equals(request.ownerStudentId())) {
			throw new ForbiddenOperationException("Projects must be created for your own student account.");
		}
		ProjectResponse response = projectService.create(request);
		return ResponseEntity.created(URI.create("/api/projects/" + response.id())).body(response);
	}

	@PutMapping("/{id}")
	public ProjectResponse update(@PathVariable Long id,
			@AuthenticationPrincipal TeamMatchPrincipal principal,
			@Valid @RequestBody ProjectCreateRequest request) {
		if (principal == null || principal.getRole() != AccountRole.STUDENT || principal.getStudentId() == null) {
			throw new ForbiddenOperationException("A student account is required to edit projects.");
		}
		return projectService.update(id, principal.getStudentId(), request);
	}

	@PatchMapping("/{id}/close")
	public ProjectResponse close(@PathVariable Long id,
			@AuthenticationPrincipal TeamMatchPrincipal principal) {
		if (principal == null || principal.getRole() != AccountRole.STUDENT || principal.getStudentId() == null) {
			throw new ForbiddenOperationException("A student account is required to close projects.");
		}
		return projectService.close(id, principal.getStudentId());
	}
}