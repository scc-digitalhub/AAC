package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfigMap;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class CredentialsAuthoritiesProperties {

    // TODO add enable//disable flag on authorities

    @NestedConfigurationProperty
    private PasswordIdentityProviderConfigMap password;

    @NestedConfigurationProperty
    private WebAuthnIdentityProviderConfigMap webauthn;

    public PasswordIdentityProviderConfigMap getPassword() {
        return password;
    }

    public void setPassword(PasswordIdentityProviderConfigMap password) {
        this.password = password;
    }

    public WebAuthnIdentityProviderConfigMap getWebauthn() {
        return webauthn;
    }

    public void setWebauthn(WebAuthnIdentityProviderConfigMap webauthn) {
        this.webauthn = webauthn;
    }
}
