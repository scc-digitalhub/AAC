package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserProfile;

@JsonInclude(Include.NON_EMPTY)
public abstract class AbstractProfile extends AbstractBaseUserResource implements UserProfile {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    static {
        mapper.setSerializationInclusion(Include.NON_EMPTY);
    }

    protected AbstractProfile(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    protected AbstractProfile(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, userId);
    }

    @Override
    @JsonIgnore
    public final String getType() {
        return SystemKeys.RESOURCE_PROFILE;
    }

    @Override
    public String getUuid() {
        return null;
    }

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
