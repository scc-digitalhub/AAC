package it.smartcommunitylab.aac.oauth.model;

import it.smartcommunitylab.aac.core.base.BaseClient;

public class OAuth2Client extends BaseClient {

    public final static String CLIENT_TYPE = "oauth2";

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getType() {
        return CLIENT_TYPE;
    }

}
