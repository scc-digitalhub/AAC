package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;

public class CustomAuthoritiesProperties {

    public String id;

    public String name;

    public String description;

    private OIDCIdentityProviderConfigMap oidc;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public OIDCIdentityProviderConfigMap getOidc() {
        return oidc;
    }

    public void setOidc(OIDCIdentityProviderConfigMap oidc) {
        this.oidc = oidc;
    }

}