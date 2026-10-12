package com.teammatch.backend.repository;

import com.teammatch.backend.entity.StudentSkill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentSkillRepository extends JpaRepository<StudentSkill, Long> {
	List<StudentSkill> findAllByStudent_Id(Long studentId);

	boolean existsByStudent_IdAndSkill_Id(Long studentId, Long skillId);
}