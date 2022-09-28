package it.smartcommunitylab.aac.openid.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

@Service
public class OIDCIdentityConfigurationProvider extends
        AbstractIdentityConfigurationProvider<OIDCIdentityProviderConfigMap, OIDCIdentityProviderConfig> {

    @Autowired
    public OIDCIdentityConfigurationProvider(IdentityAuthoritiesProperties authoritiesProperties) {
        this(SystemKeys.AUTHORITY_OIDC, new OIDCIdentityProviderConfigMap());
        if (authoritiesProperties != null && authoritiesProperties.getOidc() != null) {
            setDefaultConfigMap(authoritiesProperties.getOidc());
        }
    }

    public OIDCIdentityConfigurationProvider(String authority, OIDCIdentityProviderConfigMap configMap) {
        super(authority);
        setDefaultConfigMap(configMap);
    }

    @Override
    protected OIDCIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new OIDCIdentityProviderConfig(cp);
    }

}
