package it.smartcommunitylab.aac.profiles.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class CustomProfile extends AbstractProfile {

    @JsonIgnore
    private final String identifier;

    @JsonUnwrapped
    private final Map<String, Serializable> attributes;

    public CustomProfile(String id) {
        Assert.hasText(id, "identifier can not be null or empty");
        String identifier = id;
        if (!identifier.startsWith("profile.")) {
            identifier = "profile." + identifier;
        }

        this.identifier = identifier;
        this.attributes = new HashMap<>();

    }

    @Override
    public String getProfileId() {
        return identifier;
    }

    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    public void addAttribute(String key, Serializable value) {
        if (StringUtils.hasText(key)) {
            this.attributes.put(key, value);
        }
    }

}
