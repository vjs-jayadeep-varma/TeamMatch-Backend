package com.teammatch.backend.entity;

import com.teammatch.backend.entity.enums.SkillProficiency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "student_skills", uniqueConstraints = {
		@UniqueConstraint(name = "uk_student_skills_student_skill", columnNames = { "student_id", "skill_id" })
})
public class StudentSkill {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "skill_id", nullable = false)
	private Skill skill;

	@Enumerated(EnumType.STRING)
	@Column(name = "proficiency_level", length = 20)
	private SkillProficiency proficiencyLevel;

	protected StudentSkill() {
	}

	public StudentSkill(Student student, Skill skill, SkillProficiency proficiencyLevel) {
		this.student = student;
		this.skill = skill;
		this.proficiencyLevel = proficiencyLevel;
	}

	public Long getId() {
		return id;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public Skill getSkill() {
		return skill;
	}

	public void setSkill(Skill skill) {
		this.skill = skill;
	}

	public SkillProficiency getProficiencyLevel() {
		return proficiencyLevel;
	}

	public void setProficiencyLevel(SkillProficiency proficiencyLevel) {
		this.proficiencyLevel = proficiencyLevel;
	}
}