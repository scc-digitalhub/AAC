package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class ProvidersProperties {

    @NestedConfigurationProperty
    private List<ConfigurableIdentityProvider> identity;

    public ProvidersProperties() {
        identity = new ArrayList<>();
    }

    public List<ConfigurableIdentityProvider> getIdentity() {
        return identity;
    }

    public void setIdentity(List<ConfigurableIdentityProvider> identity) {
        this.identity = identity;
    }
}
