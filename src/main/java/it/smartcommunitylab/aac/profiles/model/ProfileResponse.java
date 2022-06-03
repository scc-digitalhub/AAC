package it.smartcommunitylab.aac.profiles.model;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import it.smartcommunitylab.aac.core.model.UserProfile;

@JsonInclude(Include.NON_EMPTY)
public class ProfileResponse {
    private String subject;
    private String realm;

    @JsonUnwrapped
    private UserProfile profile;

    public ProfileResponse(String subject) {
        Assert.notNull(subject, "subject can not be null");
        this.subject = subject;
    }

    public ProfileResponse(String subject, UserProfile profile) {
        Assert.notNull(subject, "subject can not be null");
        this.subject = subject;
        this.profile = profile;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }

}
