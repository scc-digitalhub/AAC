package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfig;
import it.smartcommunitylab.aac.internal.provider.InternalAccountServiceConfig;
import it.smartcommunitylab.aac.model.PersistenceMode;
import it.smartcommunitylab.aac.openid.apple.provider.AppleAccountServiceConfig;
import it.smartcommunitylab.aac.openid.provider.OIDCAccountServiceConfig;
import it.smartcommunitylab.aac.saml.provider.SamlAccountServiceConfig;
import org.springframework.util.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    {
        @Type(value = InternalAccountServiceConfig.class, name = InternalAccountServiceConfig.RESOURCE_TYPE),
        @Type(value = AppleAccountServiceConfig.class, name = AppleAccountServiceConfig.RESOURCE_TYPE),
        @Type(value = OIDCAccountServiceConfig.class, name = OIDCAccountServiceConfig.RESOURCE_TYPE),
        @Type(value = SamlAccountServiceConfig.class, name = SamlAccountServiceConfig.RESOURCE_TYPE),
    }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.ALWAYS)
public abstract class AbstractAccountServiceConfig<M extends AbstractConfigMap>
    extends AbstractProviderConfig<M, ConfigurableAccountProvider>
    implements AccountServiceConfig<M> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String repositoryId;
    protected PersistenceMode persistence;

    protected AbstractAccountServiceConfig(String authority, String provider, String realm, M configMap) {
        super(authority, provider, realm, configMap);
    }

    protected AbstractAccountServiceConfig(ConfigurableAccountProvider cp, M configMap) {
        super(cp, configMap);
        this.repositoryId = cp.getRepositoryId();
        this.persistence = StringUtils.hasText(cp.getPersistence()) ? PersistenceMode.parse(cp.getPersistence()) : null;
    }

    public String getRepositoryId() {
        // if undefined always use realm as default repository id
        return StringUtils.hasText(repositoryId) ? repositoryId : getRealm();
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public PersistenceMode getPersistence() {
        // by default persist to repository
        return persistence != null ? persistence : PersistenceMode.REPOSITORY;
    }

    public void setPersistence(PersistenceMode persistence) {
        this.persistence = persistence;
    }
}
