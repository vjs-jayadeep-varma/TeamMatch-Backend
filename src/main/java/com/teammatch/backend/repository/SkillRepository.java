package com.teammatch.backend.repository;

import com.teammatch.backend.entity.Skill;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
	Optional<Skill> findByName(String name);

	Optional<Skill> findFirstByNameIgnoreCaseOrderByIdAsc(String name);

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, Long id);
}