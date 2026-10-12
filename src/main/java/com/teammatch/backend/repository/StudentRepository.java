package com.teammatch.backend.repository;

import com.teammatch.backend.entity.Student;
import com.teammatch.backend.dto.AdminStudentSummary;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
	@Query("select new com.teammatch.backend.dto.AdminStudentSummary(student.id, student.name, "
			+ "student.department, student.academicYear, student.accountRole) from Student student order by student.id")
	List<AdminStudentSummary> findAllAdminSummaries();

	@Query("select student from Student student where "
			+ "(:query = '' or lower(student.name) like lower(concat('%', :query, '%')) "
			+ "or lower(student.rollNumber) like lower(concat('%', :query, '%')) "
			+ "or lower(student.email) like lower(concat('%', :query, '%')) "
			+ "or exists (select assignment.id from StudentSkill assignment join assignment.skill skill "
			+ "where assignment.student = student and lower(skill.name) like lower(concat('%', :query, '%'))) "
			+ "or exists (select interestStudent.id from Student interestStudent "
			+ "join interestStudent.interests interest where interestStudent = student "
			+ "and lower(interest) like lower(concat('%', :query, '%'))) "
			+ "or exists (select project.id from Project project where project.owner = student "
			+ "and lower(project.title) like lower(concat('%', :query, '%'))) "
			+ "or exists (select membership.id from TeamMembership membership "
			+ "join membership.team team join team.project project where membership.student = student "
			+ "and lower(project.title) like lower(concat('%', :query, '%'))))")
	Page<Student> searchAdminStudents(@Param("query") String query, Pageable pageable);

	Optional<Student> findByEmail(String email);

	Optional<Student> findByEmailIgnoreCase(String email);

	Optional<Student> findByRollNumber(String rollNumber);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	boolean existsByRollNumber(String rollNumber);

	boolean existsByRollNumberAndIdNot(String rollNumber, Long id);
}