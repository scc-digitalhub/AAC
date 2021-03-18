package it.smartcommunitylab.aac.oauth.model;

import org.springframework.security.oauth2.provider.client.BaseClientDetails;

public class OAuth2ClientDetails extends BaseClientDetails {

    private String realm;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

}
