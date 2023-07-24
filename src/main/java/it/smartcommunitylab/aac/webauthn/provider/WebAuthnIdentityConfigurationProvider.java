package it.smartcommunitylab.aac.webauthn.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import org.springframework.stereotype.Service;

@Service
public class WebAuthnIdentityConfigurationProvider
    extends AbstractIdentityConfigurationProvider<WebAuthnIdentityProviderConfigMap, WebAuthnIdentityProviderConfig> {

    public WebAuthnIdentityConfigurationProvider(IdentityAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_WEBAUTHN);
        if (authoritiesProperties != null && authoritiesProperties.getWebauthn() != null) {
            setDefaultConfigMap(authoritiesProperties.getWebauthn());
        } else {
            setDefaultConfigMap(new WebAuthnIdentityProviderConfigMap());
        }
    }

    @Override
    protected WebAuthnIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new WebAuthnIdentityProviderConfig(cp, getConfigMap(cp.getConfiguration()));
    }
}
