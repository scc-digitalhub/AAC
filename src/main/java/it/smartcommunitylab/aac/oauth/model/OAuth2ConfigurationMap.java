package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2ConfigurationMap implements ConfigurableProperties {

    private static ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, Serializable>> typeRef =
        new TypeReference<HashMap<String, Serializable>>() {};

    private Boolean enableClientRegistration;
    private Boolean openClientRegistration;

    public OAuth2ConfigurationMap() {
        enableClientRegistration = false;
        openClientRegistration = false;
    }

    public boolean getEnableClientRegistration() {
        if (enableClientRegistration != null) {
            return enableClientRegistration;
        }

        return false;
    }

    public void setEnableClientRegistration(Boolean enableClientRegistration) {
        this.enableClientRegistration = enableClientRegistration;
    }

    public boolean getOpenClientRegistration() {
        if (openClientRegistration != null) {
            return openClientRegistration;
        }

        return false;
    }

    public void setOpenClientRegistration(Boolean openClientRegistration) {
        this.openClientRegistration = openClientRegistration;
    }

    @Override
    @JsonIgnore
    public Map<String, Serializable> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(this, typeRef);
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        OAuth2ConfigurationMap map = mapper.convertValue(props, OAuth2ConfigurationMap.class);

        this.enableClientRegistration = map.getEnableClientRegistration();
        this.openClientRegistration = map.getOpenClientRegistration();
    }
}
