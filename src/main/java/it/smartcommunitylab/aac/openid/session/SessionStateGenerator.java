package it.smartcommunitylab.aac.openid.session;

/*
 * Session state generator as per
 * https://openid.net/specs/openid-connect-session-1_0.html
 */

public interface SessionStateGenerator {
    String generateState(String clientId, String originUrl, String userAgentState, String salt);
}
