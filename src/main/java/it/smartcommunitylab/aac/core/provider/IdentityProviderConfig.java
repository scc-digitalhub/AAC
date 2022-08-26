package it.smartcommunitylab.aac.core.provider;

import java.util.Map;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

public interface IdentityProviderConfig<T extends ConfigMap> extends ProviderConfig<T> {
    public String getIcon();

    public boolean isLinkable();

    public String getPersistence();

    public String getEvents();

    public Map<String, String> getHookFunctions();

    public ConfigurableIdentityProvider toConfigurableProvider();
}
