package it.smartcommunitylab.aac.templates.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.provider.TemplateProviderConfigurationProvider;

@Service
public class RealmTemplateProviderConfigurationProvider
        extends
        AbstractConfigurationProvider<TemplateProviderConfigMap, ConfigurableTemplateProvider, RealmTemplateProviderConfig>
        implements
        TemplateProviderConfigurationProvider<TemplateProviderConfigMap, RealmTemplateProviderConfig> {

    public RealmTemplateProviderConfigurationProvider() {
        super(SystemKeys.AUTHORITY_TEMPLATE);
        setDefaultConfigMap(new TemplateProviderConfigMap());
    }

    @Override
    protected RealmTemplateProviderConfig buildConfig(ConfigurableTemplateProvider cp) {
        return new RealmTemplateProviderConfig(cp, getConfigMap(cp.getConfiguration()));
    }

    @Override
    public ConfigurableTemplateProvider getConfigurable(RealmTemplateProviderConfig providerConfig) {
        ConfigurableTemplateProvider cp = new ConfigurableTemplateProvider(providerConfig.getAuthority(),
                providerConfig.getProvider(), providerConfig.getRealm());
        cp.setName(providerConfig.getName());
        cp.setTitleMap(providerConfig.getTitleMap());
        cp.setDescriptionMap(providerConfig.getDescriptionMap());

        cp.setLanguages(providerConfig.getLanguages());
        cp.setCustomStyle(providerConfig.getCustomStyle());

        cp.setConfiguration(getConfiguration(providerConfig.getConfigMap()));

        // provider config are active by definition
        cp.setEnabled(true);

        return cp;
    }

}
