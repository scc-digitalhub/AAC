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
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnIdentityProviderConfigMap implements ConfigurableProperties, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;
    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private Boolean enableRegistration = false;
    /**
     * If the user can update the credential to a new one. In this case is false by
     * default since we would create another account
     */
    private Boolean enableUpdate = false;
    /**
     * If the user can reset the credential. In our case this is false, since we can
     * not reset a WebAuthn credential
     */
    private Boolean enableReset = false;

    /*
     * if the user can confirm the email address via magic link
     */
    private Boolean enableConfirmation = false;
    private Integer confirmationValidity;

    private int maxSessionDuration = 24 * 60 * 60; // 24h

    private Boolean trustUnverifiedAuthenticatorResponses;

    public WebAuthnIdentityProviderConfigMap() {
    }

    public Boolean getEnableRegistration() {
        return enableRegistration;
    }

    public void setEnableRegistration(Boolean enableRegistration) {
        this.enableRegistration = enableRegistration;
    }

    public Boolean getEnableUpdate() {
        return enableUpdate;
    }

    public void setEnableUpdate(Boolean enableUpdate) {
        this.enableUpdate = enableUpdate;
    }

    public Boolean getEnableReset() {
        return enableReset;
    }

    public void setEnableReset(Boolean enableReset) {
        this.enableReset = enableReset;
    }

    public Boolean getEnableConfirmation() {
        return enableConfirmation;
    }

    public void setEnableConfirmation(Boolean enableConfirmation) {
        this.enableConfirmation = enableConfirmation;
    }

    public Integer getConfirmationValidity() {
        return confirmationValidity;
    }

    public void setConfirmationValidity(Integer confirmationValidity) {
        this.confirmationValidity = confirmationValidity;
    }

    public int getMaxSessionDuration() {
        return maxSessionDuration;
    }

    public void setMaxSessionDuration(int maxSessionDuration) {
        this.maxSessionDuration = maxSessionDuration;
    }

    public Boolean getTrustUnverifiedAuthenticatorResponses() {
        return trustUnverifiedAuthenticatorResponses;
    }

    public void setTrustUnverifiedAuthenticatorResponses(Boolean trustUnverifiedAuthenticatorResponses) {
        this.trustUnverifiedAuthenticatorResponses = trustUnverifiedAuthenticatorResponses;
    }

    @Override
    @JsonIgnore
    public Map<String, Serializable> getConfiguration() {
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(this, typeRef);
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        WebAuthnIdentityProviderConfigMap map = mapper.convertValue(props, WebAuthnIdentityProviderConfigMap.class);

        this.enableRegistration = map.getEnableRegistration();
        this.enableUpdate = map.getEnableUpdate();
        this.enableReset = map.getEnableReset();

        this.enableConfirmation = map.getEnableConfirmation();
        this.confirmationValidity = map.getConfirmationValidity();

        this.maxSessionDuration = map.getMaxSessionDuration();
        this.trustUnverifiedAuthenticatorResponses = map.getTrustUnverifiedAuthenticatorResponses();
    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(WebAuthnIdentityProviderConfigMap.class);
    }

}
