package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.ProjectStatus;

public record TeamProjectResponse(
		Long id,
		String title,
		String description,
		Long ownerStudentId,
		String ownerName,
		Integer requiredTeamSize,
		ProjectStatus status) {
	public TeamProjectResponse(Long id, String title, String description, Long ownerStudentId,
			Integer requiredTeamSize, ProjectStatus status) {
		this(id, title, description, ownerStudentId, null, requiredTeamSize, status);
	}
}
