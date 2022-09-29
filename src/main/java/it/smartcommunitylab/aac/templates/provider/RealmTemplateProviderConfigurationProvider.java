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
        return new RealmTemplateProviderConfig(cp);
    }

}
