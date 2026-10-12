package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.entity.enums.TeamStatus;
import java.util.List;

public record AdminProjectDetail(
		Long id,
		String title,
		String description,
		ProjectStatus status,
		Integer requiredTeamSize,
		Long ownerStudentId,
		String ownerName,
		List<String> requiredSkills,
		Long teamId,
		String teamName,
		TeamStatus teamStatus,
		List<AdminTeamMemberSummary> members) {
}
