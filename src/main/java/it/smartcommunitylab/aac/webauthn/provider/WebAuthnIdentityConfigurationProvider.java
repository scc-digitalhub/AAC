package it.smartcommunitylab.aac.webauthn.provider;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;

@Service
public class WebAuthnIdentityConfigurationProvider extends
        AbstractIdentityConfigurationProvider<WebAuthnIdentityProviderConfig, WebAuthnIdentityProviderConfigMap> {

    public WebAuthnIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_WEBAUTHN);
        if (authoritiesProperties != null && authoritiesProperties.getWebauthn() != null) {
            setDefaultConfigMap(authoritiesProperties.getWebauthn());
        } else {
            setDefaultConfigMap(new WebAuthnIdentityProviderConfigMap());
        }

    }

    @Override
    protected WebAuthnIdentityProviderConfig buildConfig(ConfigurableProvider cp) {
        Assert.isInstanceOf(ConfigurableIdentityProvider.class, cp);
        return new WebAuthnIdentityProviderConfig((ConfigurableIdentityProvider) cp);
    }

}
