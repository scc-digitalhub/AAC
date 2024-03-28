package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.spid.model.SpidIdPRegistration;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.List;

public class SpidProperties {

    @NestedConfigurationProperty
    private List<SpidIdPRegistration> identityProviders;

    public List<SpidIdPRegistration> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<SpidIdPRegistration> identityProviders) {
        this.identityProviders = identityProviders;
    }
}
