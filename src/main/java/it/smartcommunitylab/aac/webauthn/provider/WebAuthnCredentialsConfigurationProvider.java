package it.smartcommunitylab.aac.webauthn.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.CredentialsAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractCredentialsConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsService;

@Service
public class WebAuthnCredentialsConfigurationProvider extends
        AbstractCredentialsConfigurationProvider<WebAuthnIdentityProviderConfigMap, WebAuthnCredentialsServiceConfig> {

    public WebAuthnCredentialsConfigurationProvider(CredentialsAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_WEBAUTHN);
        if (authoritiesProperties != null && authoritiesProperties.getWebauthn() != null) {
            setDefaultConfigMap(authoritiesProperties.getWebauthn());
        } else {
            setDefaultConfigMap(new WebAuthnIdentityProviderConfigMap());
        }

    }

    @Override
    protected WebAuthnCredentialsServiceConfig buildConfig(ConfigurableCredentialsService cp) {
        return new WebAuthnCredentialsServiceConfig(cp);
    }

}