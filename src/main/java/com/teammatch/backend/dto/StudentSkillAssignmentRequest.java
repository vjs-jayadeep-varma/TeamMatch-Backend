package com.teammatch.backend.dto;

import com.teammatch.backend.entity.enums.SkillProficiency;
import jakarta.validation.constraints.NotNull;

public record StudentSkillAssignmentRequest(
		@NotNull Long skillId,
		SkillProficiency proficiencyLevel) {
}