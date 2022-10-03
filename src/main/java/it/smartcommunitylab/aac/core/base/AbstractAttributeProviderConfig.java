package it.smartcommunitylab.aac.core.base;

import java.util.Collections;
import java.util.Set;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProviderConfig;

public abstract class AbstractAttributeProviderConfig<M extends AbstractConfigMap>
        extends AbstractProviderConfig<M, ConfigurableAttributeProvider>
        implements AttributeProviderConfig<M> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String persistence;
    protected String events;

    protected Set<String> attributeSets;

    protected AbstractAttributeProviderConfig(String authority, String provider, String realm, M configMap) {
        super(authority, provider, realm, configMap);
        this.attributeSets = Collections.emptySet();
    }

    protected AbstractAttributeProviderConfig(ConfigurableAttributeProvider cp) {
        super(cp);

        this.persistence = cp.getPersistence();
        this.events = cp.getEvents();

        this.attributeSets = (cp.getAttributeSets() != null ? cp.getAttributeSets() : Collections.emptySet());
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
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

    public Set<String> getAttributeSets() {
        return attributeSets;
    }

    public void setAttributeSets(Set<String> attributeSets) {
        this.attributeSets = attributeSets;
    }

    @Override
    public ConfigurableAttributeProvider getConfigurable() {
        ConfigurableAttributeProvider cp = new ConfigurableAttributeProvider(getAuthority(),
                getProvider(),
                getRealm());
        cp.setType(SystemKeys.RESOURCE_ATTRIBUTES);
        cp.setPersistence(getPersistence());
        cp.setEvents(getEvents());

        cp.setName(getName());
        cp.setTitleMap(getTitleMap());
        cp.setDescriptionMap(getDescriptionMap());

        cp.setEnabled(true);
        cp.setConfiguration(getConfiguration());
        cp.setAttributeSets(attributeSets);

        return cp;
    }

}
