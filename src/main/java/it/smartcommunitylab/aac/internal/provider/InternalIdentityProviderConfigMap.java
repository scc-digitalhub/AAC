package it.smartcommunitylab.aac.internal.provider;

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
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigMap;
import it.smartcommunitylab.aac.internal.model.CredentialsType;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalIdentityProviderConfigMap extends AbstractConfigMap implements Serializable {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    @Max(3 * 24 * 60 * 60)
    protected Integer maxSessionDuration;

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String repositoryId;

    public InternalIdentityProviderConfigMap() {
    }

    public Integer getMaxSessionDuration() {
        return maxSessionDuration;
    }

    public void setMaxSessionDuration(Integer maxSessionDuration) {
        this.maxSessionDuration = maxSessionDuration;
    }

    public CredentialsType getCredentialsType() {
        return CredentialsType.NONE;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @JsonIgnore
    public void setConfiguration(InternalIdentityProviderConfigMap map) {
        this.maxSessionDuration = map.getMaxSessionDuration();
        this.repositoryId = map.getRepositoryId();
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        InternalIdentityProviderConfigMap map = mapper.convertValue(props, InternalIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(InternalIdentityProviderConfigMap.class);
    }
}
