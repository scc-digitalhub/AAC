/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.smartcommunitylab.aac.core.SessionManager;
import it.smartcommunitylab.aac.oauth.endpoint.AuthorizationEndpoint;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/*
 * Issue #689: deleting a user must destroy its spring sessions.
 * Spring Session JDBC is re-enabled here (the test profile excludes it),
 * so sessions are driven via the SESSION cookie and checked on the real
 * JDBC repository indexed by principal name (subjectId).
 * Each test deletes its own reserved user of realm "test" (del-session-*
 * in bootstrap-test.yaml), so the users shared with other tests are untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.autoconfigure.exclude=" })
public class UserSessionRevocationTest {

    private static final String REALM = "test";
    private static final String PROVIDER = "tpassword";
    private static final String PASSWORD = "test123";
    private static final String CLIENT_ID = "test-0000-12345-client";

    // shared bootstrap user, never deleted here
    private static final String SHARED_USER = "test";
    private static final String SHARED_ID = "502e7f14-b6b3-49d3-8a00-ce2ed14f7a32";

    private static final String ADMIN_DELETED_USER = "del-session-admin";
    private static final String ADMIN_DELETED_ID = "d0000001-0000-4000-8000-000000000001";
    private static final String SELF_DELETED_USER = "del-session-self";
    private static final String SELF_DELETED_ID = "d0000002-0000-4000-8000-000000000002";
    private static final String MULTI_SESSION_USER = "del-session-multi";
    private static final String MULTI_SESSION_ID = "d0000003-0000-4000-8000-000000000003";
    private static final String OTHER_USER = "del-session-other";
    private static final String OTHER_ID = "d0000004-0000-4000-8000-000000000004";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserManager userManager;

    @Autowired
    private FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    @Autowired
    private SessionManager sessionManager;

    @Test
    public void removeUserDestroysSessions_err401() throws Exception {
        Cookie session = UserSessionTestUtils.login(mockMvc, PROVIDER, ADMIN_DELETED_USER, PASSWORD);

        // session is persisted and indexed by subjectId
        assertThat(sessionRepository.findByPrincipalName(ADMIN_DELETED_ID)).hasSize(1);
        assertThat(UserSessionTestUtils.fetchUserDetails(mockMvc, session, ADMIN_DELETED_ID))
            .isEqualTo(HttpStatus.OK.value());

        UserSessionTestUtils.runAsAdmin(() -> userManager.removeUser(REALM, ADMIN_DELETED_ID));

        // session is gone and the cookie no longer authenticates
        assertThat(sessionRepository.findByPrincipalName(ADMIN_DELETED_ID)).isEmpty();
        assertThat(UserSessionTestUtils.fetchUserDetails(mockMvc, session, ADMIN_DELETED_ID))
            .isEqualTo(HttpStatus.UNAUTHORIZED.value());

        // authorize with the stale session must go to login, not issue a code
        MvcResult res = mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get(AuthorizationEndpoint.AUTHORIZATION_URL)
                    .cookie(session)
                    .param(OAuth2ParameterNames.RESPONSE_TYPE, "code")
                    .param(OAuth2ParameterNames.CLIENT_ID, CLIENT_ID)
            )
            .andExpect(status().is3xxRedirection())
            .andReturn();
        assertThat(res.getResponse().getRedirectedUrl()).contains("login").doesNotContain("code=");
    }

    @Test
    public void deleteMyUserInvalidatesCurrentSession_err401() throws Exception {
        Cookie session = UserSessionTestUtils.login(mockMvc, PROVIDER, SELF_DELETED_USER, PASSWORD);
        assertThat(sessionRepository.findByPrincipalName(SELF_DELETED_ID)).hasSize(1);

        mockMvc
            .perform(
                MockMvcRequestBuilders.delete(UserSessionTestUtils.CONSOLE_DETAILS_URL + SELF_DELETED_ID).cookie(session)
            )
            .andExpect(status().isOk());

        assertThat(sessionRepository.findByPrincipalName(SELF_DELETED_ID)).isEmpty();
        assertThat(UserSessionTestUtils.fetchUserDetails(mockMvc, session, SELF_DELETED_ID))
            .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void removeUserDestroysEverySession_err401() throws Exception {
        // same user logged in from two browsers
        Cookie first = UserSessionTestUtils.login(mockMvc, PROVIDER, MULTI_SESSION_USER, PASSWORD);
        Cookie second = UserSessionTestUtils.login(mockMvc, PROVIDER, MULTI_SESSION_USER, PASSWORD);
        assertThat(first.getValue()).isNotEqualTo(second.getValue());
        assertThat(sessionRepository.findByPrincipalName(MULTI_SESSION_ID)).hasSize(2);

        UserSessionTestUtils.runAsAdmin(() -> userManager.removeUser(REALM, MULTI_SESSION_ID));

        assertThat(sessionRepository.findByPrincipalName(MULTI_SESSION_ID)).isEmpty();
        assertThat(UserSessionTestUtils.fetchUserDetails(mockMvc, first, MULTI_SESSION_ID))
            .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(UserSessionTestUtils.fetchUserDetails(mockMvc, second, MULTI_SESSION_ID))
            .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void removeUserKeepsOtherUsersSessions() throws Exception {
        Cookie kept = UserSessionTestUtils.login(mockMvc, PROVIDER, SHARED_USER, PASSWORD);
        UserSessionTestUtils.login(mockMvc, PROVIDER, OTHER_USER, PASSWORD);

        UserSessionTestUtils.runAsAdmin(() -> userManager.removeUser(REALM, OTHER_ID));

        // only the sessions of the removed user are destroyed
        assertThat(sessionRepository.findByPrincipalName(OTHER_ID)).isEmpty();
        assertThat(sessionRepository.findByPrincipalName(SHARED_ID)).isNotEmpty();
        assertThat(UserSessionTestUtils.fetchUserDetails(mockMvc, kept, SHARED_ID)).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void destroySessionsOfUnknownUser_noError() throws Exception {
        Cookie kept = UserSessionTestUtils.login(mockMvc, PROVIDER, SHARED_USER, PASSWORD);

        // no user id, or a user without sessions: nothing to destroy and no error
        assertThatCode(() -> sessionManager.destroyUserSessions(null)).doesNotThrowAnyException();
        assertThatCode(() -> sessionManager.destroyUserSessions("unknown-user-id")).doesNotThrowAnyException();

        assertThat(sessionRepository.findByPrincipalName(SHARED_ID)).isNotEmpty();
        assertThat(UserSessionTestUtils.fetchUserDetails(mockMvc, kept, SHARED_ID)).isEqualTo(HttpStatus.OK.value());
    }
}
