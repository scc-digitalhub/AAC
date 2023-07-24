package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class AccountAuthoritiesProperties {

    // TODO add enable//disable flag on authorities

    @NestedConfigurationProperty
    private InternalIdentityProviderConfigMap internal;

    public InternalIdentityProviderConfigMap getInternal() {
        return internal;
    }

    public void setInternal(InternalIdentityProviderConfigMap internal) {
        this.internal = internal;
    }
}
