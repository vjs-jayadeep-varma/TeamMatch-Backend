package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.ProjectStatus;
import java.util.List;

public record ProjectResponse(
		Long id,
		String title,
		String description,
		Long ownerStudentId,
		String ownerName,
		Integer requiredTeamSize,
		ProjectStatus status,
		List<SkillResponse> requiredSkills,
		String teamName,
		String teamLeaderName,
		List<ProjectTeamMemberResponse> teamMembers) {
	public ProjectResponse(Long id, String title, String description, Long ownerStudentId,
			Integer requiredTeamSize, ProjectStatus status, List<SkillResponse> requiredSkills) {
		this(id, title, description, ownerStudentId, null, requiredTeamSize, status,
				requiredSkills, null, null, List.of());
	}
}