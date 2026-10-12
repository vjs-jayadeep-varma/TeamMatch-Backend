CREATE TABLE students (
	id BIGINT NOT NULL AUTO_INCREMENT,
	name VARCHAR(100) NOT NULL,
	roll_number VARCHAR(50) NOT NULL,
	email VARCHAR(254) NOT NULL,
	password_hash VARCHAR(255) NOT NULL,
	department VARCHAR(100) NOT NULL,
	academic_year INT NOT NULL,
	available BOOLEAN NOT NULL,
	account_role VARCHAR(20) NOT NULL,
	CONSTRAINT pk_students PRIMARY KEY (id),
	CONSTRAINT uk_students_roll_number UNIQUE (roll_number),
	CONSTRAINT uk_students_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE skills (
	id BIGINT NOT NULL AUTO_INCREMENT,
	name VARCHAR(100) NOT NULL,
	CONSTRAINT pk_skills PRIMARY KEY (id),
	CONSTRAINT uk_skills_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE projects (
	id BIGINT NOT NULL AUTO_INCREMENT,
	title VARCHAR(150) NOT NULL,
	description VARCHAR(2000) NOT NULL,
	owner_student_id BIGINT NOT NULL,
	required_team_size INT NOT NULL,
	status VARCHAR(20) NOT NULL,
	CONSTRAINT pk_projects PRIMARY KEY (id),
	CONSTRAINT fk_projects_owner_student
		FOREIGN KEY (owner_student_id) REFERENCES students (id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE project_required_skills (
	project_id BIGINT NOT NULL,
	skill_id BIGINT NOT NULL,
	CONSTRAINT pk_project_required_skills PRIMARY KEY (project_id, skill_id),
    CONSTRAINT uk_project_required_skill UNIQUE (project_id, skill_id),
	CONSTRAINT fk_project_required_skills_project
		FOREIGN KEY (project_id) REFERENCES projects (id),
	CONSTRAINT fk_project_required_skills_skill
		FOREIGN KEY (skill_id) REFERENCES skills (id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_skills (
	id BIGINT NOT NULL AUTO_INCREMENT,
	student_id BIGINT NOT NULL,
	skill_id BIGINT NOT NULL,
	proficiency_level VARCHAR(20),
	CONSTRAINT pk_student_skills PRIMARY KEY (id),
	CONSTRAINT uk_student_skills_student_skill UNIQUE (student_id, skill_id),
	CONSTRAINT fk_student_skills_student
		FOREIGN KEY (student_id) REFERENCES students (id),
	CONSTRAINT fk_student_skills_skill
		FOREIGN KEY (skill_id) REFERENCES skills (id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teams (
	id BIGINT NOT NULL AUTO_INCREMENT,
	project_id BIGINT NOT NULL,
	team_name VARCHAR(100) NOT NULL,
	status VARCHAR(20) NOT NULL,
	CONSTRAINT pk_teams PRIMARY KEY (id),
	CONSTRAINT uk_teams_project_id UNIQUE (project_id),
	CONSTRAINT fk_teams_project
		FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE team_memberships (
	id BIGINT NOT NULL AUTO_INCREMENT,
	team_id BIGINT NOT NULL,
	student_id BIGINT NOT NULL,
	role VARCHAR(20) NOT NULL,
	status VARCHAR(20) NOT NULL,
	CONSTRAINT pk_team_memberships PRIMARY KEY (id),
	CONSTRAINT uk_team_memberships_team_student UNIQUE (team_id, student_id),
	CONSTRAINT fk_team_memberships_team
		FOREIGN KEY (team_id) REFERENCES teams (id),
	CONSTRAINT fk_team_memberships_student
		FOREIGN KEY (student_id) REFERENCES students (id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE team_requests (
	id BIGINT NOT NULL AUTO_INCREMENT,
	requesting_student_id BIGINT NOT NULL,
	team_id BIGINT NOT NULL,
	status VARCHAR(20) NOT NULL,
	created_at TIMESTAMP(6) NOT NULL,
	updated_at TIMESTAMP(6) NOT NULL,
	CONSTRAINT pk_team_requests PRIMARY KEY (id),
	CONSTRAINT fk_team_requests_requesting_student
		FOREIGN KEY (requesting_student_id) REFERENCES students (id),
	CONSTRAINT fk_team_requests_team
		FOREIGN KEY (team_id) REFERENCES teams (id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
