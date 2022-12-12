package it.smartcommunitylab.aac.core.base;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfig;
import it.smartcommunitylab.aac.model.PersistenceMode;

public abstract class AbstractAccountServiceConfig<M extends AbstractConfigMap>
        extends AbstractProviderConfig<M, ConfigurableAccountProvider>
        implements AccountServiceConfig<M> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String repositoryId;
    protected PersistenceMode persistence;

    protected AbstractAccountServiceConfig(String authority, String provider, String realm, M configMap) {
        super(authority, provider, realm, configMap);
    }

    protected AbstractAccountServiceConfig(ConfigurableAccountProvider cp) {
        super(cp);
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

    @Override
    public ConfigurableAccountProvider getConfigurable() {
        ConfigurableAccountProvider cp = new ConfigurableAccountProvider(getAuthority(),
                getProvider(),
                getRealm());
        cp.setType(SystemKeys.RESOURCE_ACCOUNT);

        cp.setName(getName());
        cp.setTitleMap(getTitleMap());
        cp.setDescriptionMap(getDescriptionMap());

        cp.setRepositoryId(repositoryId);
        String persistenceValue = persistence != null ? persistence.getValue() : null;
        cp.setPersistence(persistenceValue);

        cp.setEnabled(true);
        cp.setConfiguration(getConfiguration());

        return cp;
    }

}
