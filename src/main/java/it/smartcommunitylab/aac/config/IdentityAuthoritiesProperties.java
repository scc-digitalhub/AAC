package it.smartcommunitylab.aac.config;

import java.util.List;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfigMap;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;

public class IdentityAuthoritiesProperties {
    // TODO add enable//disable flag on authorities

    @NestedConfigurationProperty
    private InternalIdentityProviderConfigMap internal;

    @NestedConfigurationProperty
    private PasswordIdentityProviderConfigMap password;

    @NestedConfigurationProperty
    private WebAuthnIdentityProviderConfigMap webauthn;

    @NestedConfigurationProperty
    private OIDCIdentityProviderConfigMap oidc;

    @NestedConfigurationProperty
    private SamlIdentityProviderConfigMap saml;

    @NestedConfigurationProperty
    private AppleIdentityProviderConfigMap apple;

    @NestedConfigurationProperty
    private List<CustomAuthoritiesProperties> custom;

    public InternalIdentityProviderConfigMap getInternal() {
        return internal;
    }

    public void setInternal(InternalIdentityProviderConfigMap internal) {
        this.internal = internal;
    }

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

    public OIDCIdentityProviderConfigMap getOidc() {
        return oidc;
    }

    public void setOidc(OIDCIdentityProviderConfigMap oidc) {
        this.oidc = oidc;
    }

    public SamlIdentityProviderConfigMap getSaml() {
        return saml;
    }

    public void setSaml(SamlIdentityProviderConfigMap saml) {
        this.saml = saml;
    }

    public AppleIdentityProviderConfigMap getApple() {
        return apple;
    }

    public void setApple(AppleIdentityProviderConfigMap apple) {
        this.apple = apple;
    }

    public List<CustomAuthoritiesProperties> getCustom() {
        return custom;
    }

    public void setCustom(List<CustomAuthoritiesProperties> custom) {
        this.custom = custom;
    }

}
