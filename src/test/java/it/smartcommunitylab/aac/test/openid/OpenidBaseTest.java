package it.smartcommunitylab.aac.test.openid;

import it.smartcommunitylab.aac.test.oauth.OAuth2BaseTest;

public abstract class OpenidBaseTest extends OAuth2BaseTest {

    public final static String[] SCOPES = { "profile", "openid" };

    /*
     * Helpers
     */
    @Override
    protected String[] getScopes() {
        return SCOPES;
    }
}
