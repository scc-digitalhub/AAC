package it.smartcommunitylab.aac.oauth.client;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * Additional information holder with mapping 
 * 
 * Stores extra information about oauth2 clients
 */
public class OAuth2ClientInfo {

    private static ObjectMapper mapper = new ObjectMapper();

    private String displayName;

    // TODO add extra configuration
//    static {
//        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//    }

    public static Map<String, Object> read(String additionalInformation) {
        try {
            return mapper.readValue(additionalInformation, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static OAuth2ClientInfo convert(Map<String, Object> map) {
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

}
