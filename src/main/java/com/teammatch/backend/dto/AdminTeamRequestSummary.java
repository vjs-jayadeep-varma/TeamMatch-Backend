package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.TeamRequestStatus;
import java.time.Instant;

public record AdminTeamRequestSummary(
		Long id,
		String projectTitle,
		String requestingStudentName,
		String recipientStudentName,
		TeamRequestStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
