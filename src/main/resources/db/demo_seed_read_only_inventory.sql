START TRANSACTION READ ONLY;

SELECT DATABASE() AS connected_database;

SELECT
	TABLE_NAME,
	COLUMN_NAME,
	COLUMN_TYPE,
	IS_NULLABLE,
	COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
	AND TABLE_NAME IN (
		'students', 'skills', 'student_skills', 'student_interests', 'projects',
		'project_required_skills', 'teams', 'team_memberships', 'team_requests'
	)
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT
	TABLE_NAME,
	COLUMN_NAME,
	CONSTRAINT_NAME,
	REFERENCED_TABLE_NAME,
	REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
	AND REFERENCED_TABLE_NAME IS NOT NULL
	AND TABLE_NAME IN (
		'students', 'skills', 'student_skills', 'student_interests', 'projects',
		'project_required_skills', 'teams', 'team_memberships', 'team_requests'
	)
ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION;

SELECT
	TABLE_NAME,
	CONSTRAINT_NAME,
	CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
	AND TABLE_NAME IN (
		'students', 'skills', 'student_skills', 'student_interests', 'projects',
		'project_required_skills', 'teams', 'team_memberships', 'team_requests'
	)
ORDER BY TABLE_NAME, CONSTRAINT_TYPE, CONSTRAINT_NAME;

SELECT 'students' AS table_name, COUNT(*) AS row_count FROM students
UNION ALL SELECT 'skills', COUNT(*) FROM skills
UNION ALL SELECT 'student_skills', COUNT(*) FROM student_skills
UNION ALL SELECT 'student_interests', COUNT(*) FROM student_interests
UNION ALL SELECT 'projects', COUNT(*) FROM projects
UNION ALL SELECT 'project_required_skills', COUNT(*) FROM project_required_skills
UNION ALL SELECT 'teams', COUNT(*) FROM teams
UNION ALL SELECT 'team_memberships', COUNT(*) FROM team_memberships
UNION ALL SELECT 'team_requests', COUNT(*) FROM team_requests;

SELECT id, email, roll_number, account_role, available
FROM students
WHERE email IN (
	'student1@gmail.com', 'student2@gmail.com', 'student3@gmail.com',
	'student4@gmail.com', 'student5@gmail.com', 'student6@gmail.com',
	'student7@gmail.com', 'student8@gmail.com', 'student9@gmail.com',
	'student10@gmail.com'
)
ORDER BY email;

SELECT roll_number, COUNT(*) AS existing_count
FROM students
WHERE roll_number IN (
	'DEMO-STUDENT-01', 'DEMO-STUDENT-02', 'DEMO-STUDENT-03', 'DEMO-STUDENT-04',
	'DEMO-STUDENT-05', 'DEMO-STUDENT-06', 'DEMO-STUDENT-07', 'DEMO-STUDENT-08',
	'DEMO-STUDENT-09', 'DEMO-STUDENT-10'
)
GROUP BY roll_number
ORDER BY roll_number;

SELECT id, name
FROM skills
ORDER BY name;

SELECT
	project.id,
	project.title,
	project.owner_student_id,
	CASE
		WHEN owner.email IN (
			'student1@gmail.com', 'student2@gmail.com', 'student3@gmail.com',
			'student4@gmail.com', 'student5@gmail.com', 'student6@gmail.com',
			'student7@gmail.com', 'student8@gmail.com', 'student9@gmail.com',
			'student10@gmail.com'
		) THEN owner.email
		ELSE '[non-demo account]'
	END AS owner_account,
	project.required_team_size,
	project.status,
	GROUP_CONCAT(skill.name ORDER BY skill.name SEPARATOR ', ') AS required_skills
FROM projects project
JOIN students owner ON owner.id = project.owner_student_id
LEFT JOIN project_required_skills project_skill ON project_skill.project_id = project.id
LEFT JOIN skills skill ON skill.id = project_skill.skill_id
WHERE project.title IN (
	'Java Application Development', 'Python Automation Toolkit',
	'VLSI Design and Verification', 'Machine Learning Prediction System',
	'React Student Portal', 'IoT Smart Monitoring System',
	'Signal Processing Analysis Tool', 'Embedded Systems Controller'
)
GROUP BY project.id, project.title, project.owner_student_id, owner.email,
	project.required_team_size, project.status
ORDER BY project.title;

SELECT
	team.id AS team_id,
	team.team_name,
	project.title AS project_title,
	team.status AS team_status,
	project.required_team_size,
	CASE
		WHEN member.email IN (
			'student1@gmail.com', 'student2@gmail.com', 'student3@gmail.com',
			'student4@gmail.com', 'student5@gmail.com', 'student6@gmail.com',
			'student7@gmail.com', 'student8@gmail.com', 'student9@gmail.com',
			'student10@gmail.com'
		) THEN member.email
		ELSE '[non-demo account]'
	END AS member_account,
	membership.role AS member_role,
	membership.status AS membership_status
FROM teams team
JOIN projects project ON project.id = team.project_id
LEFT JOIN team_memberships membership ON membership.team_id = team.id
LEFT JOIN students member ON member.id = membership.student_id
WHERE project.title IN (
	'Java Application Development', 'Python Automation Toolkit',
	'VLSI Design and Verification', 'Machine Learning Prediction System',
	'React Student Portal', 'IoT Smart Monitoring System',
	'Signal Processing Analysis Tool', 'Embedded Systems Controller'
)
ORDER BY project.title, member_account;

SELECT
	project.title AS project_title,
	CASE
		WHEN requester.email IN (
			'student1@gmail.com', 'student2@gmail.com', 'student3@gmail.com',
			'student4@gmail.com', 'student5@gmail.com', 'student6@gmail.com',
			'student7@gmail.com', 'student8@gmail.com', 'student9@gmail.com',
			'student10@gmail.com'
		) THEN requester.email
		ELSE '[non-demo account]'
	END AS requester_account,
	CASE
		WHEN recipient.email IN (
			'student1@gmail.com', 'student2@gmail.com', 'student3@gmail.com',
			'student4@gmail.com', 'student5@gmail.com', 'student6@gmail.com',
			'student7@gmail.com', 'student8@gmail.com', 'student9@gmail.com',
			'student10@gmail.com'
		) THEN recipient.email
		ELSE '[non-demo account]'
	END AS recipient_account,
	request_record.status,
	request_record.created_at
FROM team_requests request_record
JOIN teams team ON team.id = request_record.team_id
JOIN projects project ON project.id = team.project_id
JOIN students requester ON requester.id = request_record.requesting_student_id
JOIN students recipient ON recipient.id = request_record.recipient_student_id
WHERE project.title IN (
	'Java Application Development', 'Python Automation Toolkit',
	'VLSI Design and Verification', 'Machine Learning Prediction System',
	'React Student Portal', 'IoT Smart Monitoring System',
	'Signal Processing Analysis Tool', 'Embedded Systems Controller'
)
ORDER BY project.title, request_record.created_at;

ROLLBACK;
