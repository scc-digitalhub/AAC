package it.smartcommunitylab.aac.core.base;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountService;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfig;

public abstract class AbstractAccountServiceConfig<M extends AbstractConfigMap>
        extends AbstractProviderConfig<M, ConfigurableAccountService>
        implements AccountServiceConfig<M> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String repositoryId;

    protected AbstractAccountServiceConfig(String authority, String provider, String realm, M configMap) {
        super(authority, provider, realm, configMap);
    }

    protected AbstractAccountServiceConfig(ConfigurableAccountService cp) {
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
    public ConfigurableAccountService getConfigurable() {
        ConfigurableAccountService cp = new ConfigurableAccountService(getAuthority(),
                getProvider(),
                getRealm());
        cp.setType(SystemKeys.RESOURCE_ACCOUNT);

        cp.setName(getName());
        cp.setDescription(getDescription());

        cp.setRepositoryId(repositoryId);

        cp.setEnabled(true);
        cp.setConfiguration(getConfiguration());

        return cp;
    }

}
