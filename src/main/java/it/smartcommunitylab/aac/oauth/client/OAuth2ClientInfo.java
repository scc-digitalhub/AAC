package it.smartcommunitylab.aac.oauth.client;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * Additional information holder with mapping 
 * 
 * Stores extra information about oauth2 clients
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2ClientInfo {

    private static ObjectMapper mapper = new ObjectMapper();

    // only string properties, otherwise change method signatures to accept
    // serializable objects
    @JsonProperty("display_name")
    private String displayName;

    // TODO add extra configuration
//    static {
//        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> read(String additionalInformation) {
        try {
            return mapper.readValue(additionalInformation, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static OAuth2ClientInfo convert(Map<String, String> map) {
        return mapper.convertValue(map, OAuth2ClientInfo.class);
    }

    public String toJson() throws IllegalArgumentException {
        try {
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> toMap() throws IllegalArgumentException {
        try {
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            return mapper.convertValue(this, HashMap.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

}
