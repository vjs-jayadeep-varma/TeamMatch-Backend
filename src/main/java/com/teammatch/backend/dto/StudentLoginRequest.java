package com.teammatch.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentLoginRequest(
		@NotBlank @Size(max = 254) String email,
		@NotBlank @Size(max = 72) String password) {
}
