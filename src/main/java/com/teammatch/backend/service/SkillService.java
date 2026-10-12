package com.teammatch.backend.service;

import com.teammatch.backend.dto.SkillRequest;
import com.teammatch.backend.dto.SkillResponse;
import com.teammatch.backend.entity.Skill;
import com.teammatch.backend.exception.DuplicateResourceException;
import com.teammatch.backend.exception.ResourceNotFoundException;
import com.teammatch.backend.repository.SkillRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SkillService {
	private final SkillRepository skillRepository;

	public SkillService(SkillRepository skillRepository) {
		this.skillRepository = skillRepository;
	}

	@Transactional(readOnly = true)
	public List<SkillResponse> findAll() {
		return skillRepository.findAll().stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public SkillResponse findById(Long id) {
		return toResponse(getSkill(id));
	}

	public SkillResponse create(SkillRequest request) {
		String name = request.name().trim();
		if (skillRepository.existsByName(name)) {
			throw new DuplicateResourceException("A skill with this name already exists.");
		}

		Skill skill = new Skill(name);
		return toResponse(skillRepository.save(skill));
	}

	public SkillResponse update(Long id, SkillRequest request) {
		Skill skill = getSkill(id);
		String name = request.name().trim();
		if (skillRepository.existsByNameAndIdNot(name, id)) {
			throw new DuplicateResourceException("A skill with this name already exists.");
		}

		skill.setName(name);
		return toResponse(skillRepository.save(skill));
	}

	private Skill getSkill(Long id) {
		return skillRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Skill " + id + " was not found."));
	}

	private SkillResponse toResponse(Skill skill) {
		return new SkillResponse(skill.getId(), skill.getName());
	}
}