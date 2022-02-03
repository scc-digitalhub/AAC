package it.smartcommunitylab.aac.webauthn.provider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnIdentityProviderConfigMap implements ConfigurableProperties, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private String rpid = "https://localhost";

    private boolean enableRegistration = false;
    /**
     * If the user can update the credential to a new one. In this case is false
     * by default since we would create another account
     */
    private boolean enableUpdate = false;
    /**
     * If the user can reset the credential.
     * In our case this is false, since we can not reset a WebAuthn credential
     */
    private boolean enableReset = false;

    private int maxSessionDuration = 24 * 60 * 60; // 24h

    private boolean trustUnverifiedAuthenticatorResponses;

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        WebAuthnIdentityProviderConfigMap map = mapper.convertValue(props, WebAuthnIdentityProviderConfigMap.class);

        this.rpid = map.getRpid();
        this.enableRegistration = map.isEnableRegistration();
        this.enableUpdate = map.isEnableUpdate();
        this.trustUnverifiedAuthenticatorResponses = map.isTrustUnverifiedAuthenticatorResponses();

    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(InternalIdentityProviderConfigMap.class);
    }

    public String getRpid() {
        return rpid;
    }

    public void setRpid(String rpid) {
        this.rpid = rpid;
    }

    public void setEnableRegistration(boolean enableRegistration) {
        this.enableRegistration = enableRegistration;
    }

    public void setEnableUpdate(boolean enableUpdate) {
        this.enableUpdate = enableUpdate;
    }

    @Override
    @JsonIgnore
    public Map<String, Serializable> getConfiguration() {
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(this, typeRef);
    }

    public boolean isEnableRegistration() {
        return enableRegistration;
    }

    public boolean isEnableUpdate() {
        return enableUpdate;
    }

    public boolean isEnableReset() {
        return enableReset;
    }

    public int getMaxSessionDuration() {
        return maxSessionDuration;
    }

    public void setMaxSessionDuration(int maxSessionDuration) {
        this.maxSessionDuration = maxSessionDuration;
    }

    public boolean isTrustUnverifiedAuthenticatorResponses() {
        return trustUnverifiedAuthenticatorResponses;
    }

    public void setTrustUnverifiedAuthenticatorResponses(boolean trustUnverifiedAuthenticatorResponses) {
        this.trustUnverifiedAuthenticatorResponses = trustUnverifiedAuthenticatorResponses;
    }
}
