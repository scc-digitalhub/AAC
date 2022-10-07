package it.smartcommunitylab.aac.core.base;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.provider.IdentityServiceConfig;

public abstract class AbstractIdentityServiceConfig<M extends AbstractConfigMap>
        extends AbstractProviderConfig<M, ConfigurableIdentityService>
        implements IdentityServiceConfig<M> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String repositoryId;

    protected AbstractIdentityServiceConfig(String authority, String provider, String realm, M configMap) {
        super(authority, provider, realm, configMap);
    }

    protected AbstractIdentityServiceConfig(ConfigurableIdentityService cp) {
        super(cp);
        this.repositoryId = cp.getRepositoryId();
    }

    public String getRepositoryId() {
        // if undefined always use realm as default repository id
        return StringUtils.hasText(repositoryId) ? repositoryId : getRealm();
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Override
    public ConfigurableIdentityService getConfigurable() {
        ConfigurableIdentityService cp = new ConfigurableIdentityService(getAuthority(),
                getProvider(),
                getRealm());
        cp.setType(SystemKeys.RESOURCE_IDENTITY);

        cp.setName(getName());
        cp.setTitleMap(getTitleMap());
        cp.setDescriptionMap(getDescriptionMap());

        cp.setRepositoryId(repositoryId);

        cp.setEnabled(true);
        cp.setConfiguration(getConfiguration());

        return cp;
    }

}
