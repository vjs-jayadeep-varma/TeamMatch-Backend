package com.teammatch.backend.dto;

import java.util.List;

public record AdminMonitoringResponse(
		List<AdminStudentSummary> students,
		List<AdminProjectSummary> projects,
		List<AdminTeamSummary> teams,
		List<AdminTeamRequestSummary> teamRequests) {
}
