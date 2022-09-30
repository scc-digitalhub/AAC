package it.smartcommunitylab.aac.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.TemplateProviderAuthority;
import it.smartcommunitylab.aac.core.base.AbstractSingleProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.internal.provider.InternalTemplateProvider;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfigurationProvider;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfigMap;
import it.smartcommunitylab.aac.templates.service.TemplateService;

@Service
public class InternalTemplateAuthority extends
        AbstractSingleProviderAuthority<InternalTemplateProvider, TemplateModel, ConfigurableTemplateProvider, TemplateProviderConfigMap, RealmTemplateProviderConfig>
        implements
        TemplateProviderAuthority<InternalTemplateProvider, TemplateModel, TemplateProviderConfigMap, RealmTemplateProviderConfig> {

    // services
    private final TemplateService templateService;

    // configuration provider
    protected RealmTemplateProviderConfigurationProvider configProvider;

    public InternalTemplateAuthority(
            TemplateService templateService,
            ProviderConfigRepository<RealmTemplateProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_INTERNAL, registrationRepository);
        Assert.notNull(templateService, "template service is mandatory");

        this.templateService = templateService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_TEMPLATE;
    }

    @Autowired
    public void setConfigProvider(RealmTemplateProviderConfigurationProvider configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Override
    public RealmTemplateProviderConfigurationProvider getConfigurationProvider() {
        return configProvider;
    }

    @Override
    protected InternalTemplateProvider buildProvider(RealmTemplateProviderConfig config) {
        InternalTemplateProvider p = new InternalTemplateProvider(config.getProvider(), templateService, config,
                config.getRealm());

        return p;
    }

}
