package it.smartcommunitylab.aac.profiles.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProfile;

@JsonInclude(Include.NON_EMPTY)
public class EmailProfile extends AbstractProfile {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public static final String IDENTIFIER = "email";

    private String email;

    @JsonProperty("email_verified")
    private Boolean emailVerified;

    public EmailProfile(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, userId);
    }

    @Override
    public String getId() {
        return IDENTIFIER;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}