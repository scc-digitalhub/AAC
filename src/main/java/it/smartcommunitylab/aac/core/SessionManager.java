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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
public class SessionManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AuthenticationHelper authHelper;

    // TODO track active sessions via SessionRegistry
    @Autowired
    private SessionRegistry sessionRegistry;

    // spring session repository, sessions are indexed by principal name; missing when sessions are disabled
    @Autowired(required = false)
    private FindByIndexNameSessionRepository<? extends Session> sessionRepository;

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
        // we revoke sessions but not tokens, those should be handled elsewhere
        // note: SessionInformation.expireNow() only flags the session, we need to delete it
        if (sessionRepository == null || userId == null) {
            logger.debug("session repository or user id not available, skip destroy sessions for user {}", userId);
            return;
        }

        Map<String, ? extends Session> sessions = sessionRepository.findByPrincipalName(userId);
        logger.debug("destroy {} sessions for user {}", sessions.size(), userId);
        sessions.keySet().forEach(sessionRepository::deleteById);
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
