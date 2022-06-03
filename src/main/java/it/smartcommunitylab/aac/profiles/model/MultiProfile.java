package it.smartcommunitylab.aac.profiles.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProfile;
import it.smartcommunitylab.aac.core.model.UserProfile;

@JsonInclude(Include.NON_EMPTY)
public class MultiProfile<T extends UserProfile> extends AbstractProfile {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;
    private static final String DEFAULT_KEY = "profiles";

    @JsonIgnore
    private final String id;

    @JsonIgnore
    private String key;

    @JsonIgnore
    private List<T> profiles;

    public MultiProfile(String authority, String provider, String realm, String userId, String id) {
        super(authority, provider, realm, userId);

        Assert.hasText(id, "identifier can not be null or empty");
        String identifier = id;
        if (!identifier.startsWith("profile.")) {
            identifier = "profile." + identifier;
        }

        this.id = identifier;
        this.profiles = new ArrayList<>();
        this.key = DEFAULT_KEY;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @JsonAnyGetter
    public Map<String, List<T>> getProfilesMap() {
        return Collections.singletonMap(this.key, this.profiles);
    }

    public List<T> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<T> profiles) {
        this.profiles = new ArrayList<>();
        if (profiles != null) {
            this.profiles.addAll(profiles);
        }
    }

    public void addProfile(T profile) {
        if (profile != null) {
            profiles.add(profile);
        }
    }

}
