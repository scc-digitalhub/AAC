package it.smartcommunitylab.aac.saml.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;

@Service
public class SamlIdentityConfigurationProvider
        extends AbstractIdentityConfigurationProvider<SamlIdentityProviderConfig, SamlIdentityProviderConfigMap> {

    @Autowired
    public SamlIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
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
    protected SamlIdentityProviderConfig buildConfig(ConfigurableProvider cp) {
        Assert.isInstanceOf(ConfigurableIdentityProvider.class, cp);
        return new SamlIdentityProviderConfig((ConfigurableIdentityProvider) cp);
    }

}
