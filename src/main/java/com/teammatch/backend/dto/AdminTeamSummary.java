package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.TeamStatus;
import java.util.List;

public record AdminTeamSummary(
		Long id, String name, TeamStatus status, Long projectId, String projectTitle,
		List<AdminTeamMemberSummary> members) {
}
