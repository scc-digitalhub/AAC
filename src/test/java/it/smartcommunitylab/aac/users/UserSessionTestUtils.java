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
import it.smartcommunitylab.aac.password.PasswordIdentityAuthority;
import javax.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/*
 * Helpers for session tests: real password login via the auth filter chain,
 * session cookie replay against the user console and admin execution context.
 */
public class UserSessionTestUtils {

    public static final String SESSION_COOKIE = "SESSION";
    public static final String LOGIN_URL = PasswordIdentityAuthority.AUTHORITY_URL + "login/";
    public static final String CONSOLE_DETAILS_URL = "/console/user/details/";

    private UserSessionTestUtils() {}

    /*
     * Login with username/password on the given provider and return the SESSION cookie
     */
    public static Cookie login(MockMvc mockMvc, String providerId, String username, String password)
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

        Cookie cookie = res.getResponse().getCookie(SESSION_COOKIE);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    /*
     * Fetch current user details from console using only the session cookie, return http status
     */
    public static int fetchUserDetails(MockMvc mockMvc, Cookie session, String userId) throws Exception {
        MvcResult res = mockMvc
            .perform(MockMvcRequestBuilders.get(CONSOLE_DETAILS_URL + userId).cookie(session))
            .andReturn();
        return res.getResponse().getStatus();
    }

    /*
     * Run the action with an admin authentication in the security context, then clear it
     */
    public static void runAsAdmin(ThrowingAction action) throws Exception {
        TestingAuthenticationToken admin = new TestingAuthenticationToken("admin", "", Config.R_ADMIN);
        admin.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(admin);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @FunctionalInterface
    public interface ThrowingAction {
        void run() throws Exception;
    }
}
