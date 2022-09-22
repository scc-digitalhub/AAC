package it.smartcommunitylab.aac.saml.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

@Service
public class SamlIdentityConfigurationProvider
        extends AbstractIdentityConfigurationProvider<SamlIdentityProviderConfigMap, SamlIdentityProviderConfig> {

    @Autowired
    public SamlIdentityConfigurationProvider(IdentityAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_SAML);
        if (authoritiesProperties != null && authoritiesProperties.getSaml() != null) {
            setDefaultConfigMap(authoritiesProperties.getSaml());
        } else {
            setDefaultConfigMap(new SamlIdentityProviderConfigMap());
        }
    }

    public SamlIdentityConfigurationProvider(String authority, SamlIdentityProviderConfigMap configMap) {
        super(authority);
        setDefaultConfigMap(configMap);
    }

    @Override
    protected SamlIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new SamlIdentityProviderConfig(cp);
    }

}
