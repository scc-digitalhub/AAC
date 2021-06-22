package it.smartcommunitylab.aac.profiles.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.smartcommunitylab.aac.Config;

public class SessionProfile extends AbstractProfile {

    public static final String IDENTIFIER = "profile.session.me";

    // a composite key for the idp (authority+providerId) responsible for the last
    // authentication
    @JsonProperty("idp")
    private String provider;

    @JsonProperty("login_at")
    private long loginAt;

    @JsonProperty("ipaddr")
    private String remoteAddress;

    // defines if authentication is current or fetched from store
    // for example for refreshed tokens
    @JsonProperty("active")
    private boolean active;

    // describes when authentication userDetails is refreshed before token
    // generation
    @JsonProperty("refreshed")
    private boolean refreshed;

    /*
     * Authentication context info.
     * 
     * see openId profile
     * https://openid.net/specs/openid-connect-modrna-authentication-1_0.html
     * 
     * we don't follow the spec here but provide our representation. A dedicated
     * profile should be developed to adhere to standard
     * 
     */

    // TODO derive from security context claims
    // these make sense only with support for 2FA or smart card etc..
    private String amr;

    // authentication context class reference:
    // level of assurance is related to provider: spid/cie etc can produce a
    // loa1+, otherwise we keep loa0
    private int acr = 0;

    @Override
    public String getProfileId() {
        return IDENTIFIER;
    }
}
