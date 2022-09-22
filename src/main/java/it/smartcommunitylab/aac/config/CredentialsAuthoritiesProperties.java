package it.smartcommunitylab.aac.config;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import it.smartcommunitylab.aac.password.provider.PasswordCredentialsServiceConfigMap;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsServiceConfigMap;

public class CredentialsAuthoritiesProperties {
    // TODO add enable//disable flag on authorities

    @NestedConfigurationProperty
    private PasswordCredentialsServiceConfigMap password;

    @NestedConfigurationProperty
    private WebAuthnCredentialsServiceConfigMap webauthn;

    public PasswordCredentialsServiceConfigMap getPassword() {
        return password;
    }

    public void setPassword(PasswordCredentialsServiceConfigMap password) {
        this.password = password;
    }

    public WebAuthnCredentialsServiceConfigMap getWebauthn() {
        return webauthn;
    }

    public void setWebauthn(WebAuthnCredentialsServiceConfigMap webauthn) {
        this.webauthn = webauthn;
    }

}
