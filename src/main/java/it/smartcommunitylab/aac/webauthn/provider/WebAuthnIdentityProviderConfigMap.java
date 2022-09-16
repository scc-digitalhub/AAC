package it.smartcommunitylab.aac.webauthn.provider;

import java.io.Serializable;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserVerificationRequirement;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigMap;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnIdentityProviderConfigMap extends AbstractConfigMap implements Serializable {
    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    @Max(3 * 24 * 60 * 60)
    protected Integer maxSessionDuration;

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String repositoryId;

    private Boolean displayAsButton;

    private Boolean allowUntrustedAttestation;
    private UserVerificationRequirement requireUserVerification;
    private ResidentKeyRequirement requireResidentKey;

    private Integer loginTimeout;

    public WebAuthnIdentityProviderConfigMap() {
    }

    public Integer getMaxSessionDuration() {
        return maxSessionDuration;
    }

    public void setMaxSessionDuration(Integer maxSessionDuration) {
        this.maxSessionDuration = maxSessionDuration;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Boolean getDisplayAsButton() {
        return displayAsButton;
    }

    public void setDisplayAsButton(Boolean displayAsButton) {
        this.displayAsButton = displayAsButton;
    }

    public Boolean getAllowUntrustedAttestation() {
        return allowUntrustedAttestation;
    }

    public void setAllowUntrustedAttestation(Boolean allowUntrustedAttestation) {
        this.allowUntrustedAttestation = allowUntrustedAttestation;
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

    public Integer getLoginTimeout() {
        return loginTimeout;
    }

    public void setLoginTimeout(Integer loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    @JsonIgnore
    public void setConfiguration(WebAuthnIdentityProviderConfigMap map) {
        this.maxSessionDuration = map.getMaxSessionDuration();
        this.repositoryId = map.getRepositoryId();

        this.displayAsButton = map.getDisplayAsButton();
        this.allowUntrustedAttestation = map.getAllowUntrustedAttestation();

        this.requireResidentKey = map.getRequireResidentKey();
        this.requireUserVerification = map.getRequireUserVerification();

        this.loginTimeout = map.getLoginTimeout();
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper for local
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        WebAuthnIdentityProviderConfigMap map = mapper.convertValue(props, WebAuthnIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(WebAuthnIdentityProviderConfigMap.class);
    }

}
