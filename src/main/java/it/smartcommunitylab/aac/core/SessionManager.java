package it.smartcommunitylab.aac.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SessionManager {

    @Autowired
    private AuthenticationHelper authHelper;

    // TODO track active sessions via SessionRegistry

    /*
     * User sessions
     */
    public void listUserSessions() {

    }

    public void listUserSessions(String userId) {

    }

    public void destroyUserSessions(String userId) {
        // destroy sessions for users
        // we revoke session but not tokens, those should be handled eslewhere
    }

    /*
     * Client sessions
     */
    public void listClientSessions() {

    }

    public void listClientSessions(String clientId) {

    }

    public void destroyClientSessions(String clientId) {
        // destroy sessions for clients
        // we revoke session but not tokens
    }

    /*
     * Realm sessions
     */

    public void listRealmSessions(String realm) {

    }

    public void destroyRealmSessions(String realm) {
        // destroy sessions from the given provider
    }

    /*
     * Provider sessions
     */

    public void listProviderSessions(String providerId) {

    }

    public void destroyProviderSessions(String providerId) {
        // destroy sessions from the given provider
    }

}
