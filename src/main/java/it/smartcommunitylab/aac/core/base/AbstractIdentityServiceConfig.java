package it.smartcommunitylab.aac.core.base;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.provider.IdentityServiceConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityServiceConfig;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = InternalIdentityServiceConfig.class, name = InternalIdentityServiceConfig.RESOURCE_TYPE),
})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.ALWAYS)
public abstract class AbstractIdentityServiceConfig<M extends AbstractConfigMap>
        extends AbstractProviderConfig<M, ConfigurableIdentityService>
        implements IdentityServiceConfig<M> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String repositoryId;

    protected AbstractIdentityServiceConfig(String authority, String provider, String realm, M configMap) {
        super(authority, provider, realm, configMap);
    }

    protected AbstractIdentityServiceConfig(ConfigurableIdentityService cp, M configMap) {
        super(cp, configMap);
        this.repositoryId = cp.getRepositoryId();
    }

    public String getRepositoryId() {
        // if undefined always use realm as default repository id
        return StringUtils.hasText(repositoryId) ? repositoryId : getRealm();
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

}
