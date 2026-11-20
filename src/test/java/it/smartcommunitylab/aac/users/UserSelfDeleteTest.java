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

import it.smartcommunitylab.aac.audit.listeners.UserEventListener;
import it.smartcommunitylab.aac.audit.store.AuditEventStore;
import it.smartcommunitylab.aac.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/*
 * Self delete from the user console sends two requests on the same session:
 * the delete and the logout. These tests fix what the server does for each
 * order of arrival, and which user a delete request can reach.
 * Each test uses its own reserved user of realm "test" (del-self-* in
 * bootstrap-test.yaml), so the users shared with other tests are untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserSelfDeleteTest {

    private static final String PROVIDER = "tpassword";
    private static final String PASSWORD = "test123";

    // shared bootstrap user, never deleted here
    private static final String SHARED_ID = "502e7f14-b6b3-49d3-8a00-ce2ed14f7a32";

    private static final String LOGOUT_FIRST_USER = "del-self-late";
    private static final String LOGOUT_FIRST_ID = "d000000c-0000-4000-8000-00000000000c";
    private static final String DELETE_FIRST_USER = "del-self-first";
    private static final String DELETE_FIRST_ID = "d000000d-0000-4000-8000-00000000000d";
    private static final String OTHER_PATH_USER = "del-self-path";
    private static final String OTHER_PATH_ID = "d000000e-0000-4000-8000-00000000000e";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditEventStore auditStore;

    @Test
    public void deleteAfterLogout_err401() throws Exception {
        MockHttpSession session = UserDeleteTestUtils.login(mockMvc, PROVIDER, LOGOUT_FIRST_USER, PASSWORD);

        // the logout reaches the server before the delete
        UserDeleteTestUtils.logout(mockMvc, session);
        int status = UserDeleteTestUtils.deleteUser(mockMvc, session, LOGOUT_FIRST_ID);

        // the delete is refused: the user is still there and nothing is recorded
        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(userService.findUser(LOGOUT_FIRST_ID)).isNotNull();
        assertThat(auditStore.findByPrincipal(LOGOUT_FIRST_ID, null, null, UserEventListener.USER_DELETE)).isEmpty();
    }

    @Test
    public void deleteBeforeLogout_userDeleted() throws Exception {
        MockHttpSession session = UserDeleteTestUtils.login(mockMvc, PROVIDER, DELETE_FIRST_USER, PASSWORD);

        // the delete reaches the server before the logout
        int status = UserDeleteTestUtils.deleteUser(mockMvc, session, DELETE_FIRST_ID);
        UserDeleteTestUtils.logout(mockMvc, session);

        assertThat(status).isEqualTo(HttpStatus.OK.value());
        assertThat(userService.findUser(DELETE_FIRST_ID)).isNull();
        assertThat(UserDeleteTestUtils.awaitEvent(auditStore, DELETE_FIRST_ID, UserEventListener.USER_DELETE))
            .isNotNull();
    }

    @Test
    public void deleteWithoutSession_err401() throws Exception {
        // no session at all: the request can not reach any user
        int status = UserDeleteTestUtils.deleteUser(mockMvc, null, SHARED_ID);

        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(userService.findUser(SHARED_ID)).isNotNull();
    }

    @Test
    public void deleteWithOtherUserId_deletesCurrentUser() throws Exception {
        MockHttpSession session = UserDeleteTestUtils.login(mockMvc, PROVIDER, OTHER_PATH_USER, PASSWORD);

        // the id in the path is not used: only the user of the session can be deleted
        int status = UserDeleteTestUtils.deleteUser(mockMvc, session, SHARED_ID);

        assertThat(status).isEqualTo(HttpStatus.OK.value());
        assertThat(userService.findUser(SHARED_ID)).isNotNull();
        assertThat(userService.findUser(OTHER_PATH_ID)).isNull();
    }
}
