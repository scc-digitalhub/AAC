package it.smartcommunitylab.aac.core.provider;

import java.util.Map;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.model.PersistenceMode;

public interface IdentityProviderConfig<M extends ConfigMap> extends ProviderConfig<M> {

    public boolean isLinkable();

    public PersistenceMode getPersistence();

    public String getEvents();

    public Integer getPosition();

    public Map<String, String> getHookFunctions();

}
