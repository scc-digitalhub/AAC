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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.smartcommunitylab.aac.audit.listeners.UserEventListener;
import it.smartcommunitylab.aac.audit.store.AuditEventStore;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/*
 * A user delete is recorded in the audit as USER_DELETE: the deleted user is
 * the principal, the data carry realm and who performed the delete, and no
 * other personal data.
 * Each test uses its own reserved user of realm "test" (del-audit-* in
 * bootstrap-test.yaml), so the users shared with other tests are untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserDeleteAuditTest {

    private static final String REALM = "test";
    private static final String PROVIDER = "tpassword";
    private static final String PASSWORD = "test123";

    private static final String ADMIN_DELETED_ID = "d000000a-0000-4000-8000-00000000000a";
    private static final String SELF_DELETED_USER = "del-audit-self";
    private static final String SELF_DELETED_ID = "d000000b-0000-4000-8000-00000000000b";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserManager userManager;

    @Autowired
    private AuditEventStore auditStore;

    @Test
    public void userRemoval_auditUserDelete() throws Exception {
        UserDeleteTestUtils.runAsAdmin(() -> userManager.removeUser(REALM, ADMIN_DELETED_ID));

        AuditEvent event = UserDeleteTestUtils.awaitEvent(auditStore, ADMIN_DELETED_ID, UserEventListener.USER_DELETE);
        assertThat(event).isNotNull();
        assertThat(event.getData()).containsEntry("realm", REALM);
        assertThat(event.getData()).doesNotContainKey("username");
        assertThat(event.getData()).containsEntry("actor", UserDeleteTestUtils.ADMIN);
    }

    @Test
    public void selfDelete_auditUserDelete() throws Exception {
        // the user deletes its own account from the console
        MockHttpSession session = UserDeleteTestUtils.login(mockMvc, PROVIDER, SELF_DELETED_USER, PASSWORD);
        assertThat(UserDeleteTestUtils.deleteUser(mockMvc, session, SELF_DELETED_ID)).isEqualTo(HttpStatus.OK.value());

        AuditEvent event = UserDeleteTestUtils.awaitEvent(auditStore, SELF_DELETED_ID, UserEventListener.USER_DELETE);
        assertThat(event).isNotNull();
        assertThat(event.getData()).containsEntry("realm", REALM);
        assertThat(event.getData()).doesNotContainKey("username");
        assertThat(event.getData()).containsEntry("actor", SELF_DELETED_ID);
    }

    @Test
    public void missingUserRemoval_errNoSuchUser() throws Exception {
        String missingId = "d00000ff-0000-4000-8000-0000000000ff";

        // nothing is deleted, so nothing is recorded
        assertThatThrownBy(() -> UserDeleteTestUtils.runAsAdmin(() -> userManager.removeUser(REALM, missingId)))
            .isInstanceOf(NoSuchUserException.class);
        assertThat(auditStore.findByPrincipal(missingId, null, null, UserEventListener.USER_DELETE)).isEmpty();
    }
}
