package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.TeamRequestStatus;
import java.time.Instant;

public record AdminTeamRequestDetail(
		Long id,
		Long projectId,
		String projectTitle,
		Long teamId,
		String teamName,
		Long requestingStudentId,
		String requestingStudentName,
		String requestType,
		Long recipientStudentId,
		String recipientStudentName,
		TeamRequestStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
