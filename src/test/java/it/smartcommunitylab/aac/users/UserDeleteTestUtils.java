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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.audit.store.AuditEventStore;
import it.smartcommunitylab.aac.password.PasswordIdentityAuthority;
import java.util.List;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/*
 * Helpers for user delete tests: real password login returning the http
 * session, logout and delete calls replaying that session, admin execution
 * context for manager calls and polling of the (asynchronous) audit store.
 */
public class UserDeleteTestUtils {

    public static final String LOGIN_URL = PasswordIdentityAuthority.AUTHORITY_URL + "login/";
    public static final String LOGOUT_URL = "/logout";
    public static final String CONSOLE_DETAILS_URL = "/console/user/details/";
    public static final String ADMIN = "admin";

    private static final int ATTEMPTS = 50;
    private static final long DELAY_MILLIS = 100;

    private UserDeleteTestUtils() {}

    /*
     * Login with username/password on the given provider and return the authenticated session
     */
    public static MockHttpSession login(MockMvc mockMvc, String providerId, String username, String password)
        throws Exception {
        MvcResult res = mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post(LOGIN_URL + providerId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", username)
                    .param("password", password)
            )
            .andExpect(status().is3xxRedirection())
            .andReturn();

        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    /*
     * Logout as the browser does: a plain GET with the session, which is invalidated
     */
    public static void logout(MockMvc mockMvc, MockHttpSession session) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(LOGOUT_URL).session(session)).andExpect(status().is3xxRedirection());
    }

    /*
     * Ask the user console to delete the user with the given session (may be null), return http status
     */
    public static int deleteUser(MockMvc mockMvc, MockHttpSession session, String userId) throws Exception {
        var req = MockMvcRequestBuilders.delete(CONSOLE_DETAILS_URL + userId);
        if (session != null) {
            req = req.session(session);
        }
        return mockMvc.perform(req).andReturn().getResponse().getStatus();
    }

    /*
     * Run the action with an admin authentication in the security context, then clear it
     */
    public static void runAsAdmin(ThrowingAction action) throws Exception {
        TestingAuthenticationToken admin = new TestingAuthenticationToken(ADMIN, "", Config.R_ADMIN);
        admin.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(admin);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /*
     * Wait for the first event of the given type for the principal, null when none is stored in time
     */
    public static AuditEvent awaitEvent(AuditEventStore store, String principal, String type)
        throws InterruptedException {
        for (int i = 0; i < ATTEMPTS; i++) {
            List<AuditEvent> events = store.findByPrincipal(principal, null, null, type);
            if (!events.isEmpty()) {
                return events.get(0);
            }
            Thread.sleep(DELAY_MILLIS);
        }
        return null;
    }

    @FunctionalInterface
    public interface ThrowingAction {
        void run() throws Exception;
    }
}
