package it.smartcommunitylab.aac.core.base;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfig;

public abstract class AbstractCredentialsServiceConfig<M extends AbstractConfigMap>
        extends AbstractProviderConfig<M, ConfigurableCredentialsProvider>
        implements CredentialsServiceConfig<M> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String repositoryId;

    protected AbstractCredentialsServiceConfig(String authority, String provider, String realm, M configMap) {
        super(authority, provider, realm, configMap);
    }

    protected AbstractCredentialsServiceConfig(ConfigurableCredentialsProvider cp) {
        super(cp);

    }

    public String getRepositoryId() {
        return StringUtils.hasText(repositoryId) ? repositoryId : getRealm();
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Override
    public ConfigurableCredentialsProvider getConfigurable() {
        ConfigurableCredentialsProvider cs = new ConfigurableCredentialsProvider(getAuthority(),
                getProvider(),
                getRealm());
        cs.setType(SystemKeys.RESOURCE_CREDENTIALS);

        cs.setName(getName());
        cs.setTitleMap(getTitleMap());
        cs.setDescriptionMap(getDescriptionMap());

        cs.setRepositoryId(repositoryId);

        cs.setEnabled(true);
        cs.setConfiguration(getConfiguration());

        return cs;
    }

}
