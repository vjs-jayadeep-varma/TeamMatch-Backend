package com.teammatch.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProjectCreateRequest(
		@NotBlank @Size(max = 150) String title,
		@NotBlank @Size(max = 2000) String description,
		@NotNull @Positive Long ownerStudentId,
		@NotNull @Min(2) @Max(20) Integer requiredTeamSize,
		@NotNull @Size(max = 20) List<@NotNull @Positive Long> requiredSkillIds) {
}