package it.smartcommunitylab.aac.core.base;

import java.util.Collections;
import java.util.Set;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

public abstract class AbstractAttributeProviderConfig extends AbstractProviderConfig {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected Set<String> attributeSets;

    protected AbstractAttributeProviderConfig(String authority, String provider, String realm) {
        super(authority, provider, realm);
        this.attributeSets = Collections.emptySet();
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    public Set<String> getAttributeSets() {
        return attributeSets;
    }

    public void setAttributeSets(Set<String> attributeSets) {
        this.attributeSets = attributeSets;
    }

    public ConfigurableAttributeProvider toConfigurableProvider() {
        ConfigurableAttributeProvider cp = new ConfigurableAttributeProvider(getAuthority(),
                getProvider(),
                getRealm());
        cp.setType(SystemKeys.RESOURCE_ATTRIBUTES);
        cp.setName(getName());
        cp.setDescription(getDescription());

        cp.setEnabled(true);
        cp.setConfiguration(getConfiguration());
        cp.setAttributeSets(attributeSets);

        return cp;
    }

}
