package it.smartcommunitylab.aac.profiles.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(Include.NON_EMPTY)
public abstract class AbstractProfile implements Serializable {

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    static {
        mapper.setSerializationInclusion(Include.NON_EMPTY);
    }

    @JsonIgnore
    public abstract String getProfileId();

    /*
     * Convert profile, subclasses can override
     */

    public String toJson() throws IllegalArgumentException {
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public HashMap<String, Serializable> toMap() throws IllegalArgumentException {
        try {
            return mapper.convertValue(this, typeRef);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

}
