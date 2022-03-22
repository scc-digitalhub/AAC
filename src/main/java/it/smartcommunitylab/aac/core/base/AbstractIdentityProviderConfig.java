package it.smartcommunitylab.aac.core.base;

import java.util.Collections;
import java.util.Map;

import it.smartcommunitylab.aac.SystemKeys;

public abstract class AbstractIdentityProviderConfig extends AbstractProviderConfig {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String name;
    protected String description;
    protected String icon;
    protected String displayMode;

    protected Boolean linkable;
    protected String persistence;
    protected String events;

    protected Map<String, String> hookFunctions;

    protected AbstractIdentityProviderConfig(String authority, String provider, String realm) {
        super(authority, provider, realm);
        this.hookFunctions = Collections.emptyMap();
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(String displayMode) {
        this.displayMode = displayMode;
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
        cp.setPersistence(getPersistence());

        cp.setName(getName());
        cp.setDescription(getDescription());
        cp.setIcon(getIcon());
        cp.setDisplayMode(getDisplayMode());

        cp.setEnabled(true);
        cp.setLinkable(isLinkable());
        cp.setConfiguration(getConfiguration());
        cp.setHookFunctions(getHookFunctions());

        return cp;
    }

}
