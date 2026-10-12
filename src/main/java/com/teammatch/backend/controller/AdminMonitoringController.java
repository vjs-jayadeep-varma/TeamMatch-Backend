package com.teammatch.backend.controller;

import com.teammatch.backend.dto.AdminMonitoringResponse;
import com.teammatch.backend.dto.AdminOverviewResponse;
import com.teammatch.backend.dto.AdminPage;
import com.teammatch.backend.dto.AdminProjectDetail;
import com.teammatch.backend.dto.AdminProjectSummary;
import com.teammatch.backend.dto.AdminStudentDetail;
import com.teammatch.backend.dto.AdminStudentListItem;
import com.teammatch.backend.dto.AdminTeamRequestDetail;
import com.teammatch.backend.dto.AdminTeamSummary;
import com.teammatch.backend.entity.enums.TeamRequestStatus;
import com.teammatch.backend.service.AdminMonitoringService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminMonitoringController {
	private final AdminMonitoringService adminMonitoringService;

	public AdminMonitoringController(AdminMonitoringService adminMonitoringService) {
		this.adminMonitoringService = adminMonitoringService;
	}

	@GetMapping("/monitoring")
	public AdminMonitoringResponse getMonitoringData() {
		return adminMonitoringService.getMonitoringData();
	}

	@GetMapping("/overview")
	public AdminOverviewResponse getOverview() {
		return adminMonitoringService.getOverview();
	}

	@GetMapping("/students")
	public AdminPage<AdminStudentListItem> findStudents(
			@RequestParam(defaultValue = "") String query,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return adminMonitoringService.findStudents(normalizeQuery(query), page, size);
	}

	@GetMapping("/students/{studentId}")
	public AdminStudentDetail findStudent(@PathVariable Long studentId) {
		return adminMonitoringService.findStudent(studentId);
	}

	@GetMapping("/projects")
	public AdminPage<AdminProjectSummary> findProjects(
			@RequestParam(defaultValue = "") String query,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return adminMonitoringService.findProjects(normalizeQuery(query), page, size);
	}

	@GetMapping("/projects/{projectId}")
	public AdminProjectDetail findProject(@PathVariable Long projectId) {
		return adminMonitoringService.findProject(projectId);
	}

	@GetMapping("/teams")
	public AdminPage<AdminTeamSummary> findTeams(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return adminMonitoringService.findTeams(page, size);
	}

	@GetMapping("/team-requests")
	public AdminPage<AdminTeamRequestDetail> findRequests(
			@RequestParam(required = false) TeamRequestStatus status,
			@RequestParam(required = false) Long projectId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return adminMonitoringService.findRequests(status, projectId, page, size);
	}

	private String normalizeQuery(String query) {
		String trimmed = query.trim();
		return trimmed.substring(0, Math.min(trimmed.length(), 100));
	}
}
