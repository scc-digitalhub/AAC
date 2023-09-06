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

package it.smartcommunitylab.aac.core;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.Subject;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

@Service
public class SessionManager {

    @Autowired
    private AuthenticationHelper authHelper;

    // TODO track active sessions via SessionRegistry
    @Autowired
    private SessionRegistry sessionRegistry;

    /*
     * User sessions
     */
    public void listUserSessions() {}

    public List<SessionInformation> listUserSessions(String userId, String realm, String name) {
        // build a principal and search
        Subject principal = new Subject(userId, realm, name, SystemKeys.RESOURCE_USER);
        //        return sessionRegistry.getAllSessions(principal, false);
        List<Object> principals = sessionRegistry.getAllPrincipals();
        List<SessionInformation> result = new ArrayList<>();
        for (Object p : principals) {
            result.addAll(sessionRegistry.getAllSessions(p, false));
        }
        return result;
    }

    public void destroyUserSessions(String userId) {
        // destroy sessions for users
        // we revoke session but not tokens, those should be handled eslewhere
    }

    /*
     * Client sessions
     */
    public void listClientSessions() {}

    public void listClientSessions(String clientId) {}

    public void destroyClientSessions(String clientId) {
        // destroy sessions for clients
        // we revoke session but not tokens
    }

    /*
     * Realm sessions
     */

    public void listRealmSessions(String realm) {}

    public void destroyRealmSessions(String realm) {
        // destroy sessions from the given provider
    }

    /*
     * Provider sessions
     */

    public void listProviderSessions(String providerId) {}

    public void destroyProviderSessions(String providerId) {
        // destroy sessions from the given provider
    }
}
