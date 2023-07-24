package it.smartcommunitylab.aac.profiles.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.smartcommunitylab.aac.SystemKeys;

@JsonInclude(Include.NON_EMPTY)
public class SessionProfile extends AbstractProfile {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public static final String IDENTIFIER = "session";

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
    private Boolean active;

    // describes when authentication userDetails is refreshed before token
    // generation
    @JsonProperty("refreshed")
    private Boolean refreshed;

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
    private Integer acr = 0;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public long getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(long loginAt) {
        this.loginAt = loginAt;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getRefreshed() {
        return refreshed;
    }

    public void setRefreshed(Boolean refreshed) {
        this.refreshed = refreshed;
    }

    public String getAmr() {
        return amr;
    }

    public void setAmr(String amr) {
        this.amr = amr;
    }

    public Integer getAcr() {
        return acr;
    }

    public void setAcr(Integer acr) {
        this.acr = acr;
    }
}
