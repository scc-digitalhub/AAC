package it.smartcommunitylab.aac.profiles.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;
import java.util.HashMap;

@JsonInclude(Include.NON_EMPTY)
public abstract class AbstractProfile implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    private static ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, Serializable>> typeRef =
        new TypeReference<HashMap<String, Serializable>>() {};

    static {
        mapper.setSerializationInclusion(Include.NON_EMPTY);
    }

    public String getId() {
        StringBuilder sb = new StringBuilder();
        sb.append(getIdentifier());
        return sb.toString();
    }

    @JsonIgnore
    public abstract String getIdentifier();

    /*
     * Convert profile, subclasses can override
     */

    @JsonIgnore
    public String toJson() throws IllegalArgumentException {
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @JsonIgnore
    public HashMap<String, Serializable> toMap() throws IllegalArgumentException {
        try {
            return mapper.convertValue(this, typeRef);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
