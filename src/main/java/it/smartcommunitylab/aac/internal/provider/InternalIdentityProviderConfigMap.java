package it.smartcommunitylab.aac.internal.provider;

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

import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalIdentityProviderConfigMap implements ConfigurableProperties {

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    private boolean enableRegistration = true;
    private boolean enableDelete = true;
    private boolean enableUpdate = true;

    private boolean enablePasswordReset = true;
    private boolean enablePasswordSet = true;
    private int passwordResetValidity;

    private boolean confirmationRequired = true;
    private int confirmationValidity;

    // password policy, optional
    private int passwordMinLength;
    private int passwordMaxLength;
    private boolean passwordRequireAlpha;
    private boolean passwordRequireNumber;
    private boolean passwordRequireSpecial;
    private boolean passwordSupportWhitespace;

    public InternalIdentityProviderConfigMap() {

    }

    public boolean isEnableRegistration() {
        return enableRegistration;
    }

    public void setEnableRegistration(boolean enableRegistration) {
        this.enableRegistration = enableRegistration;
    }

    public boolean isEnableDelete() {
        return enableDelete;
    }

    public void setEnableDelete(boolean enableDelete) {
        this.enableDelete = enableDelete;
    }

    public boolean isEnableUpdate() {
        return enableUpdate;
    }

    public void setEnableUpdate(boolean enableUpdate) {
        this.enableUpdate = enableUpdate;
    }

    public boolean isEnablePasswordReset() {
        return enablePasswordReset;
    }

    public void setEnablePasswordReset(boolean enablePasswordReset) {
        this.enablePasswordReset = enablePasswordReset;
    }

    public boolean isEnablePasswordSet() {
        return enablePasswordSet;
    }

    public void setEnablePasswordSet(boolean enablePasswordSet) {
        this.enablePasswordSet = enablePasswordSet;
    }

    public int getPasswordResetValidity() {
        return passwordResetValidity;
    }

    public void setPasswordResetValidity(int passwordResetValidity) {
        this.passwordResetValidity = passwordResetValidity;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    public int getConfirmationValidity() {
        return confirmationValidity;
    }

    public void setConfirmationValidity(int confirmationValidity) {
        this.confirmationValidity = confirmationValidity;
    }

    public int getPasswordMinLength() {
        return passwordMinLength;
    }

    public void setPasswordMinLength(int passwordMinLength) {
        this.passwordMinLength = passwordMinLength;
    }

    public int getPasswordMaxLength() {
        return passwordMaxLength;
    }

    public void setPasswordMaxLength(int passwordMaxLength) {
        this.passwordMaxLength = passwordMaxLength;
    }

    public boolean isPasswordRequireAlpha() {
        return passwordRequireAlpha;
    }

    public void setPasswordRequireAlpha(boolean passwordRequireAlpha) {
        this.passwordRequireAlpha = passwordRequireAlpha;
    }

    public boolean isPasswordRequireNumber() {
        return passwordRequireNumber;
    }

    public void setPasswordRequireNumber(boolean passwordRequireNumber) {
        this.passwordRequireNumber = passwordRequireNumber;
    }

    public boolean isPasswordRequireSpecial() {
        return passwordRequireSpecial;
    }

    public void setPasswordRequireSpecial(boolean passwordRequireSpecial) {
        this.passwordRequireSpecial = passwordRequireSpecial;
    }

    public boolean isPasswordSupportWhitespace() {
        return passwordSupportWhitespace;
    }

    public void setPasswordSupportWhitespace(boolean passwordSupportWhitespace) {
        this.passwordSupportWhitespace = passwordSupportWhitespace;
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
        InternalIdentityProviderConfigMap map = mapper.convertValue(props, InternalIdentityProviderConfigMap.class);

        this.confirmationRequired = map.isConfirmationRequired();
        this.enableRegistration = map.isEnableRegistration();
        this.enableDelete = map.isEnableDelete();
        this.enableUpdate = map.isEnableUpdate();

        this.enablePasswordReset = map.isEnablePasswordReset();
        this.enablePasswordSet = map.isEnablePasswordSet();
        this.passwordResetValidity = map.getPasswordResetValidity();

        this.confirmationRequired = map.isConfirmationRequired();
        this.confirmationValidity = map.getConfirmationValidity();

        // password policy, optional
        this.passwordMinLength = map.getPasswordMinLength();
        this.passwordMaxLength = map.getPasswordMaxLength();
        this.passwordRequireAlpha = map.isPasswordRequireAlpha();
        this.passwordRequireNumber = map.isPasswordRequireNumber();
        this.passwordRequireSpecial = map.isPasswordRequireSpecial();
        this.passwordSupportWhitespace = map.isPasswordSupportWhitespace();

    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(InternalIdentityProviderConfigMap.class);
    }
}
