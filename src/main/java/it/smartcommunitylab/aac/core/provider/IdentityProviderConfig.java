package it.smartcommunitylab.aac.core.provider;

import java.util.Map;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

public interface IdentityProviderConfig<M extends ConfigMap> extends ProviderConfig<M, ConfigurableIdentityProvider> {

    public String getIcon();

    public boolean isLinkable();

    public String getPersistence();

    public String getEvents();

    public Map<String, String> getHookFunctions();

}
