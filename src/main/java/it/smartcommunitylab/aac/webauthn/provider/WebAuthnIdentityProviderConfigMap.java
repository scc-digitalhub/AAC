package it.smartcommunitylab.aac.webauthn.provider;

import java.io.Serializable;
import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserVerificationRequirement;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.model.CredentialsType;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnIdentityProviderConfigMap extends InternalIdentityProviderConfigMap {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    private Boolean displayAsButton;

    private Boolean trustUnverifiedAuthenticatorResponses;

    private UserVerificationRequirement requireUserVerification;

    private ResidentKeyRequirement requireResidentKey;

    public WebAuthnIdentityProviderConfigMap() {
        super();
        this.credentialsType = CredentialsType.WEBAUTHN;
    }

    @Override
    public CredentialsType getCredentialsType() {
        return CredentialsType.WEBAUTHN;
    }

    public Boolean getDisplayAsButton() {
        return displayAsButton;
    }

    public void setDisplayAsButton(Boolean displayAsButton) {
        this.displayAsButton = displayAsButton;
    }

    public Boolean getTrustUnverifiedAuthenticatorResponses() {
        return trustUnverifiedAuthenticatorResponses;
    }

    public void setTrustUnverifiedAuthenticatorResponses(Boolean trustUnverifiedAuthenticatorResponses) {
        this.trustUnverifiedAuthenticatorResponses = trustUnverifiedAuthenticatorResponses;
    }

    protected UserVerificationRequirement getRequireUserVerification() {
        return requireUserVerification;
    }

    protected void setRequireUserVerification(UserVerificationRequirement requireUserVerification) {
        this.requireUserVerification = requireUserVerification;
    }

    protected ResidentKeyRequirement getRequireResidentKey() {
        return requireResidentKey;
    }

    protected void setRequireResidentKey(ResidentKeyRequirement requireResidentKey) {
        this.requireResidentKey = requireResidentKey;
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
        // set base via super
        super.setConfiguration(props);

        // use mapper for local
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        WebAuthnIdentityProviderConfigMap map = mapper.convertValue(props, WebAuthnIdentityProviderConfigMap.class);
        this.credentialsType = CredentialsType.WEBAUTHN;

        this.displayAsButton = map.getDisplayAsButton();
        this.trustUnverifiedAuthenticatorResponses = map.getTrustUnverifiedAuthenticatorResponses();
        this.requireResidentKey = map.getRequireResidentKey();
        this.requireUserVerification = map.getRequireUserVerification();
    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(WebAuthnIdentityProviderConfigMap.class);
    }

}
