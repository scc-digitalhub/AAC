package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import java.util.List;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class SpidProperties {

    @NestedConfigurationProperty
    private List<SpidRegistration> identityProviders;

    public List<SpidRegistration> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<SpidRegistration> identityProviders) {
        this.identityProviders = identityProviders;
    }
}
