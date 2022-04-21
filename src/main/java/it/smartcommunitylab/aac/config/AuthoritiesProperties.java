package it.smartcommunitylab.aac.config;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;

public class AuthoritiesProperties {

    @NestedConfigurationProperty
    private InternalIdentityProviderConfigMap internal;

    public InternalIdentityProviderConfigMap getInternal() {
        return internal;
    }

    public void setInternal(InternalIdentityProviderConfigMap internal) {
        this.internal = internal;
    }

}
