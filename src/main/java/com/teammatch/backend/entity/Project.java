package com.teammatch.backend.entity;

import com.teammatch.backend.entity.enums.ProjectStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
public class Project {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 150)
	@Column(nullable = false, length = 150)
	private String title;

	@NotBlank
	@Size(max = 2000)
	@Column(nullable = false, length = 2000)
	private String description;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_student_id", nullable = false)
	private Student owner;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "project_required_skills",
			joinColumns = @JoinColumn(name = "project_id"),
			inverseJoinColumns = @JoinColumn(name = "skill_id"),
			uniqueConstraints = @UniqueConstraint(name = "uk_project_required_skill", columnNames = { "project_id", "skill_id" }))
	private Set<Skill> requiredSkills = new HashSet<>();

	@NotNull
	@Min(2)
	@Max(20)
	@Column(name = "required_team_size", nullable = false)
	private Integer requiredTeamSize;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectStatus status = ProjectStatus.DRAFT;

	protected Project() {
	}

	public Project(String title, String description, Student owner, Set<Skill> requiredSkills,
			Integer requiredTeamSize, ProjectStatus status) {
		this.title = title;
		this.description = description;
		this.owner = owner;
		this.requiredSkills = requiredSkills;
		this.requiredTeamSize = requiredTeamSize;
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Student getOwner() {
		return owner;
	}

	public void setOwner(Student owner) {
		this.owner = owner;
	}

	public Set<Skill> getRequiredSkills() {
		return requiredSkills;
	}

	public void setRequiredSkills(Set<Skill> requiredSkills) {
		this.requiredSkills = requiredSkills;
	}

	public Integer getRequiredTeamSize() {
		return requiredTeamSize;
	}

	public void setRequiredTeamSize(Integer requiredTeamSize) {
		this.requiredTeamSize = requiredTeamSize;
	}

	public ProjectStatus getStatus() {
		return status;
	}

	public void setStatus(ProjectStatus status) {
		this.status = status;
	}
}