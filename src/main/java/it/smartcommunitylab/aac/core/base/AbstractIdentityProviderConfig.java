package it.smartcommunitylab.aac.core.base;

import java.util.Collections;
import java.util.Map;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

public abstract class AbstractIdentityProviderConfig<T extends AbstractConfigMap> extends AbstractProviderConfig<T> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String icon;

    protected Boolean linkable;
    protected String persistence;
    protected String events;
    protected Integer position;

    protected Map<String, String> hookFunctions;

    protected AbstractIdentityProviderConfig(String authority, String provider, String realm, T configMap) {
        super(authority, provider, realm, configMap);
        this.hookFunctions = Collections.emptyMap();
    }

    protected AbstractIdentityProviderConfig(ConfigurableIdentityProvider cp) {
        super(cp);

        this.icon = cp.getIcon();

        this.persistence = cp.getPersistence();
        this.linkable = cp.isLinkable();
        this.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    public ConfigurableIdentityProvider toConfigurableProvider() {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider(getAuthority(),
                getProvider(),
                getRealm());
        cp.setType(SystemKeys.RESOURCE_IDENTITY);

        cp.setName(getName());
        cp.setDescription(getDescription());
        cp.setIcon(getIcon());

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
