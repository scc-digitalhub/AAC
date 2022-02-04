package it.smartcommunitylab.aac.profiles.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;

@JsonInclude(Include.NON_EMPTY)
public class EmailProfile extends AbstractProfile {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public static final String IDENTIFIER = "email";

    private String email;

    @JsonProperty("email_verified")
    private Boolean emailVerified;

    public EmailProfile() {
    }

    public EmailProfile(BasicProfile p) {
        email = p.getEmail();
        emailVerified = false;
    }

    @Override
    public String getIdentifier() {
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