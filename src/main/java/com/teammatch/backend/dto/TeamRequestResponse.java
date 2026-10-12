package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.TeamRequestStatus;
import java.time.Instant;

public record TeamRequestResponse(
		Long id,
		Long projectId,
		String projectTitle,
		String teamName,
		String requestType,
		Long requestingStudentId,
		String requestingStudentName,
		Long recipientStudentId,
		String recipientStudentName,
		TeamRequestStatus status,
		Instant createdAt,
		Instant updatedAt) {
	public TeamRequestResponse(Long id, Long projectId, String projectTitle,
			Long requestingStudentId, String requestingStudentName,
			Long recipientStudentId, String recipientStudentName, TeamRequestStatus status,
			Instant createdAt, Instant updatedAt) {
		this(id, projectId, projectTitle, null, null, requestingStudentId, requestingStudentName,
				recipientStudentId, recipientStudentName, status, createdAt, updatedAt);
	}
}
