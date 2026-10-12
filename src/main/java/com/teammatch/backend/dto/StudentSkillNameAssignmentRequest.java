package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.SkillProficiency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentSkillNameAssignmentRequest(
		@NotBlank @Size(max = 100) String skillName,
		SkillProficiency proficiencyLevel) {
}
