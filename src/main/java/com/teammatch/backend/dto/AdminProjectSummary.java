package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.ProjectStatus;

public record AdminProjectSummary(
		Long id, String title, ProjectStatus status, Integer requiredTeamSize, String ownerName) {
}
