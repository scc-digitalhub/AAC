package it.smartcommunitylab.aac.config;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import it.smartcommunitylab.aac.internal.provider.InternalAccountServiceConfigMap;

public class AccountAuthoritiesProperties {
    // TODO add enable//disable flag on authorities

    @NestedConfigurationProperty
    private InternalAccountServiceConfigMap internal;

    public InternalAccountServiceConfigMap getInternal() {
        return internal;
    }

    public void setInternal(InternalAccountServiceConfigMap internal) {
        this.internal = internal;
    }

}
