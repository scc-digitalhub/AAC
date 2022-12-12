package it.smartcommunitylab.aac.core.base;

import java.util.Collections;
import java.util.Map;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfig;
import it.smartcommunitylab.aac.model.PersistenceMode;

public abstract class AbstractIdentityProviderConfig<M extends AbstractConfigMap>
        extends AbstractProviderConfig<M, ConfigurableIdentityProvider>
        implements IdentityProviderConfig<M> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected Boolean linkable;
    protected PersistenceMode persistence;
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
        this.persistence = StringUtils.hasText(cp.getPersistence()) ? PersistenceMode.parse(cp.getPersistence()) : null;
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

    public PersistenceMode getPersistence() {
        // by default persist to repository
        return persistence != null ? persistence : PersistenceMode.REPOSITORY;
    }

    public void setPersistence(PersistenceMode persistence) {
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
        String persistenceValue = persistence != null ? persistence.getValue() : null;
        cp.setPersistence(persistenceValue);
        cp.setEvents(getEvents());
        cp.setPosition(getPosition());

        cp.setEnabled(true);
        cp.setConfiguration(getConfiguration());
        cp.setHookFunctions(getHookFunctions());

        return cp;
    }

}
