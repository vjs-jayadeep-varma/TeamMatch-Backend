package com.teammatch.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teammatch.backend.dto.SkillRequest;
import com.teammatch.backend.dto.SkillResponse;
import com.teammatch.backend.entity.Skill;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {
	@Mock
	private SkillRepository skillRepository;

	@InjectMocks
	private SkillService skillService;

	@Test
	void createTrimsSkillName() {
		when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));

		SkillResponse response = skillService.create(new SkillRequest("  Java  "));

		assertEquals("Java", response.name());
	}

	@Test
	void createRejectsDuplicateNameBeforeSaving() {
		when(skillRepository.existsByName("Java")).thenReturn(true);

		assertThrows(DuplicateResourceException.class,
				() -> skillService.create(new SkillRequest("Java")));
		verify(skillRepository, never()).save(any(Skill.class));
	}
}