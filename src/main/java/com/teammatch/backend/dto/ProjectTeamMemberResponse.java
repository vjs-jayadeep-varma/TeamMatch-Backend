package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.TeamMemberRole;

public record ProjectTeamMemberResponse(Long studentId, String name, TeamMemberRole role) {
}
