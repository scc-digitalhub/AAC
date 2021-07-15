package it.smartcommunitylab.aac.profiles.model;

import java.util.Collection;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonInclude(Include.NON_EMPTY)
public class ProfileResponse {
    private String subject;
    private String realm;

    @JsonUnwrapped
    private AbstractProfile profile;

    @JsonUnwrapped
    private Collection<AbstractProfile> profiles;

    public ProfileResponse(String subject) {
        Assert.notNull(subject, "subject can not be null");
        this.subject = subject;
    }

    public ProfileResponse(String subject, AbstractProfile profile) {
        Assert.notNull(subject, "subject can not be null");
        this.subject = subject;
        this.profile = profile;
    }

    public ProfileResponse(String subject, Collection<AbstractProfile> profiles) {
        Assert.notNull(subject, "subject can not be null");
        this.subject = subject;
        if (profiles != null) {
            if (profiles.size() == 1) {
                this.profile = profiles.iterator().next();
            } else {
                this.profiles = profiles;
            }
        }
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

    public AbstractProfile getProfile() {
        return profile;
    }

    public void setProfile(AbstractProfile profile) {
        this.profile = profile;
    }

    public Collection<AbstractProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(Collection<AbstractProfile> profiles) {
        if (profiles != null && profiles.size() == 1) {
            this.profile = profiles.iterator().next();
            this.profiles = null;
        } else {
            this.profiles = profiles;
        }

    }

}
