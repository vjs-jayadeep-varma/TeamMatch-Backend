package com.teammatch.backend.controller;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teammatch.backend.config.DevSecurityConfiguration;
import com.teammatch.backend.config.PasswordEncoderConfiguration;
import com.teammatch.backend.dto.StudentTeamResponse;
import com.teammatch.backend.dto.TeamMemberResponse;
import com.teammatch.backend.dto.TeamProjectResponse;
import com.teammatch.backend.entity.enums.AccountRole;
import com.teammatch.backend.entity.enums.MembershipStatus;
import com.teammatch.backend.entity.enums.ProjectStatus;
import com.teammatch.backend.entity.enums.TeamMemberRole;
import com.teammatch.backend.entity.enums.TeamStatus;
import com.teammatch.backend.repository.StudentRepository;
import com.teammatch.backend.security.TeamMatchPrincipal;
import com.teammatch.backend.service.StudentTeamService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentTeamController.class)
@Import({ DevSecurityConfiguration.class, PasswordEncoderConfiguration.class })
@ActiveProfiles("dev")
@TestPropertySource(properties = {
		"TEAMMATCH_DEV_USERNAME=test-user",
		"TEAMMATCH_DEV_PASSWORD=test-only-development-password"
})
class StudentTeamControllerTest {
	private static final String TEST_USERNAME = "test-user";
	private static final String TEST_PASSWORD = "test-only-development-password";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StudentTeamService studentTeamService;

	@MockitoBean
	private StudentRepository studentRepository;

	@Test
	void returnsTeamsForAuthenticatedStudent() throws Exception {
		when(studentTeamService.findTeams(5L, 5L)).thenReturn(List.of(response()));

		mockMvc.perform(get("/api/students/5/teams").with(user(principal(5L))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].teamId").value(20))
				.andExpect(jsonPath("$[0].project.title").value("Team Project"))
				.andExpect(jsonPath("$[0].members[0].name").value("Member"));
	}

	@Test
	void rejectsStudentIdDifferentFromAuthenticatedIdentity() throws Exception {
		mockMvc.perform(get("/api/students/6/teams").with(user(principal(5L))))
				.andExpect(status().isForbidden());
	}

	@Test
	void sharedDevelopmentUserCannotReadStudentTeams() throws Exception {
		mockMvc.perform(get("/api/students/5/teams")
					.with(httpBasic(TEST_USERNAME, TEST_PASSWORD)))
				.andExpect(status().isForbidden());
	}

	private TeamMatchPrincipal principal(Long studentId) {
		TeamMatchPrincipal principal = mock(TeamMatchPrincipal.class);
		when(principal.getStudentId()).thenReturn(studentId);
		when(principal.getUsername()).thenReturn("student" + studentId + "@example.com");
		when(principal.getPassword()).thenReturn("encoded-password-hash");
		when(principal.getRole()).thenReturn(AccountRole.STUDENT);
		doReturn(List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))).when(principal).getAuthorities();
		return principal;
	}

	private StudentTeamResponse response() {
		TeamMemberResponse member = new TeamMemberResponse(5L, "Member", "member@example.com", "CSE", 2,
				TeamMemberRole.MEMBER, MembershipStatus.ACTIVE);
		TeamProjectResponse project = new TeamProjectResponse(10L, "Team Project", "Description", 1L, 4,
				ProjectStatus.IN_PROGRESS);
		return new StudentTeamResponse(20L, "Team Project Team", TeamStatus.FORMING, TeamMemberRole.MEMBER,
				MembershipStatus.ACTIVE, project, List.of(member));
	}
}
