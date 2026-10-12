package com.teammatch.backend.controller;

import com.teammatch.backend.dto.SkillRequest;
import com.teammatch.backend.dto.SkillResponse;
import com.teammatch.backend.service.SkillService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skills")
public class SkillController {
	private final SkillService skillService;

	public SkillController(SkillService skillService) {
		this.skillService = skillService;
	}

	@GetMapping
	public List<SkillResponse> findAll() {
		return skillService.findAll();
	}

	@GetMapping("/{id}")
	public SkillResponse findById(@PathVariable Long id) {
		return skillService.findById(id);
	}

	@PostMapping
	public ResponseEntity<SkillResponse> create(@Valid @RequestBody SkillRequest request) {
		SkillResponse response = skillService.create(request);
		return ResponseEntity.created(URI.create("/api/skills/" + response.id())).body(response);
	}

	@PutMapping("/{id}")
	public SkillResponse update(@PathVariable Long id, @Valid @RequestBody SkillRequest request) {
		return skillService.update(id, request);
	}
}