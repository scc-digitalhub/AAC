package it.smartcommunitylab.aac.profiles.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(Include.NON_EMPTY)
public abstract class AbstractProfile implements Serializable {

    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(Include.NON_EMPTY);
    }

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

    @SuppressWarnings("unchecked")
    public HashMap<String, String> toMap() throws IllegalArgumentException {
        try {
            return mapper.convertValue(this, HashMap.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

}
