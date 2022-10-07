package it.smartcommunitylab.aac.core.base;

import java.util.Locale;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfig;

public abstract class AbstractConfigurableProvider<R extends Resource, T extends ConfigurableProvider, M extends ConfigMap, C extends ProviderConfig<M, T>>
        extends AbstractProvider<R> implements ConfigurableResourceProvider<R, T, M, C> {

    protected final C config;

    protected AbstractConfigurableProvider(String authority, String provider, String realm, C providerConfig) {
        super(authority, provider, realm);
        Assert.notNull(providerConfig, "provider config can not be null");

        // check configuration
        Assert.isTrue(provider.equals(providerConfig.getProvider()), "configuration does not match this provider");
        Assert.isTrue(realm.equals(providerConfig.getRealm()), "configuration does not match this provider");

        this.config = providerConfig;
    }

    @Override
    public C getConfig() {
        return config;
    }

    @Override
    public T getConfigurable() {
        return getConfig().getConfigurable();
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getTitle(Locale locale) {
        String lang = locale.getLanguage();
        if (config.getTitleMap() != null) {
            return config.getTitleMap().get(lang);
        }

        return null;
    }

    @Override
    public String getDescription(Locale locale) {
        String lang = locale.getLanguage();
        if (config.getDescriptionMap() != null) {
            return config.getDescriptionMap().get(lang);
        }

        return null;
    }

    public Map<String, String> getTitleMap() {
        return config.getTitleMap();
    }

    public Map<String, String> getDescriptionMap() {
        return config.getDescriptionMap();
    }
}
