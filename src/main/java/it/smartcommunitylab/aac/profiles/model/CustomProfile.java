package it.smartcommunitylab.aac.profiles.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;

@JsonInclude(Include.NON_EMPTY)
public class CustomProfile extends AbstractProfile {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    @JsonIgnore
    private final String identifier;

    // attributes map should be kept internal, anyGetter will ensure values are
    // extracted one by one. jsonUnwrapped does not work for convertValue
    @JsonIgnore
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
    public String getIdentifier() {
        return identifier;
    }

    @JsonAnyGetter
    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    public void addAttribute(String key, Serializable value) {
        if (StringUtils.hasText(key)) {
            this.attributes.put(key, value);
        }
    }

}
