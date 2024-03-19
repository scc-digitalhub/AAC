package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpidIdentityConfigurationProvider extends AbstractIdentityConfigurationProvider<SpidIdentityProviderConfig, SpidIdentityProviderConfigMap> {

    @Autowired
    public SpidIdentityConfigurationProvider(
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
        IdentityAuthoritiesProperties authoritiesProperties
    ) {
        this(SystemKeys.AUTHORITY_SPID, registrationRepository, new IdentityProviderSettingsMap(), new SpidIdentityProviderConfigMap());
        if (
                authoritiesProperties != null &&
                authoritiesProperties.getSettings() != null &&
                authoritiesProperties.getSpid() != null
        ) {
            setDefaultSettingsMap(authoritiesProperties.getSettings());
            setDefaultConfigMap(authoritiesProperties.getSpid());
        }
    }

    public SpidIdentityConfigurationProvider(
        String authority,
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
        IdentityProviderSettingsMap settings,
        SpidIdentityProviderConfigMap configs
    ) {
        super(authority, registrationRepository);
        setDefaultSettingsMap(settings);
        setDefaultConfigMap(configs);
    }

    @Override
    protected SpidIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new SpidIdentityProviderConfig(cp, getSettingsMap(cp.getSettings()), getConfigMap(cp.getConfiguration()));
    }
}
