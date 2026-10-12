package com.teammatch.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TeamRequestCreateRequest(
		@NotNull(message = "A recipient student ID is required.")
		@Positive(message = "The recipient student ID must be positive.")
		Long recipientStudentId) {
}
