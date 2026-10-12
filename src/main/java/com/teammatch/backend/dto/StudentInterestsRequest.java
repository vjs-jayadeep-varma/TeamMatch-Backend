package com.teammatch.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record StudentInterestsRequest(
		@NotNull @Size(max = 20) List<@NotBlank @Size(max = 100) String> interests) {
}
