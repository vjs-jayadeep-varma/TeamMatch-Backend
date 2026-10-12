package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.TeamRequestStatus;
import jakarta.validation.constraints.NotNull;

public record TeamRequestDecisionRequest(
		@NotNull TeamRequestStatus status) {
}
