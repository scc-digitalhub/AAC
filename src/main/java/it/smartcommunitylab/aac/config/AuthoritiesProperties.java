package it.smartcommunitylab.aac.config;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;
import it.smartcommunitylab.aac.password.provider.InternalPasswordIdentityProviderConfigMap;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;

public class AuthoritiesProperties {
    // TODO add enable//disable flag on authorities

    @NestedConfigurationProperty
    private InternalIdentityProviderConfigMap internal;

    @NestedConfigurationProperty
    private InternalPasswordIdentityProviderConfigMap password;

    @NestedConfigurationProperty
    private WebAuthnIdentityProviderConfigMap webauthn;

    @NestedConfigurationProperty
    private OIDCIdentityProviderConfigMap oidc;

    @NestedConfigurationProperty
    private SamlIdentityProviderConfigMap saml;

    @NestedConfigurationProperty
    private AppleIdentityProviderConfigMap apple;

    @NestedConfigurationProperty
    private SpidIdentityProviderConfigMap spid;

    public InternalIdentityProviderConfigMap getInternal() {
        return internal;
    }

    public void setInternal(InternalIdentityProviderConfigMap internal) {
        this.internal = internal;
    }

    public InternalPasswordIdentityProviderConfigMap getPassword() {
        return password;
    }

    public void setPassword(InternalPasswordIdentityProviderConfigMap password) {
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

    public SpidIdentityProviderConfigMap getSpid() {
        return spid;
    }

    public void setSpid(SpidIdentityProviderConfigMap spid) {
        this.spid = spid;
    }

}
