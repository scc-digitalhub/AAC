package it.smartcommunitylab.aac.core.base;

import java.util.Collections;
import java.util.Map;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfig;

public abstract class AbstractIdentityProviderConfig<M extends AbstractConfigMap>
        extends AbstractProviderConfig<M, ConfigurableIdentityProvider>
        implements IdentityProviderConfig<M> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected Boolean linkable;
    protected String persistence;
    protected String events;
    protected Integer position;

    protected Map<String, String> hookFunctions;

    protected AbstractIdentityProviderConfig(String authority, String provider, String realm, M configMap) {
        super(authority, provider, realm, configMap);
        this.hookFunctions = Collections.emptyMap();
    }

    protected AbstractIdentityProviderConfig(ConfigurableIdentityProvider cp) {
        super(cp);

        this.linkable = cp.isLinkable();
        this.persistence = cp.getPersistence();
        this.events = cp.getEvents();
        this.position = cp.getPosition();

        this.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());
    }

    public Boolean getLinkable() {
        return linkable;
    }

    public void setLinkable(Boolean linkable) {
        this.linkable = linkable;
    }

    public boolean isLinkable() {
        return linkable != null ? linkable.booleanValue() : true;
    }

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    @Override
    public ConfigurableIdentityProvider getConfigurable() {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider(getAuthority(),
                getProvider(),
                getRealm());
        cp.setType(SystemKeys.RESOURCE_IDENTITY);

        cp.setName(getName());
        cp.setTitleMap(getTitleMap());
        cp.setDescriptionMap(getDescriptionMap());

        cp.setLinkable(isLinkable());
        cp.setPersistence(getPersistence());
        cp.setEvents(getEvents());
        cp.setPosition(getPosition());

        cp.setEnabled(true);
        cp.setConfiguration(getConfiguration());
        cp.setHookFunctions(getHookFunctions());

        return cp;
    }

}
