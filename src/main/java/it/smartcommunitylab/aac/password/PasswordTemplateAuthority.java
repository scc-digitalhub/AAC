package it.smartcommunitylab.aac.password;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.TemplateProviderAuthority;
import it.smartcommunitylab.aac.core.base.AbstractSingleConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.password.provider.PasswordTemplateProvider;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfigurationProvider;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfigMap;
import it.smartcommunitylab.aac.templates.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class PasswordTemplateAuthority
    extends AbstractSingleConfigurableProviderAuthority<PasswordTemplateProvider, TemplateModel, ConfigurableTemplateProvider, TemplateProviderConfigMap, RealmTemplateProviderConfig>
    implements
        TemplateProviderAuthority<PasswordTemplateProvider, TemplateModel, TemplateProviderConfigMap, RealmTemplateProviderConfig> {

    // services
    private final TemplateService templateService;

    // configuration provider
    protected RealmTemplateProviderConfigurationProvider configProvider;

    public PasswordTemplateAuthority(
        TemplateService templateService,
        ProviderConfigRepository<RealmTemplateProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_PASSWORD, new PasswordConfigTranslatorRepository(registrationRepository));
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
    protected PasswordTemplateProvider buildProvider(RealmTemplateProviderConfig config) {
        PasswordTemplateProvider p = new PasswordTemplateProvider(
            config.getProvider(),
            templateService,
            config,
            config.getRealm()
        );

        return p;
    }

    static class PasswordConfigTranslatorRepository
        extends TranslatorProviderConfigRepository<RealmTemplateProviderConfig, RealmTemplateProviderConfig> {

        public PasswordConfigTranslatorRepository(
            ProviderConfigRepository<RealmTemplateProviderConfig> externalRepository
        ) {
            super(externalRepository);
            setConverter(config -> {
                RealmTemplateProviderConfig c = new RealmTemplateProviderConfig(
                    SystemKeys.AUTHORITY_PASSWORD,
                    config.getProvider(),
                    config.getRealm(),
                    config.getConfigMap()
                );
                c.setCustomStyle(config.getCustomStyle());
                c.setLanguages(c.getLanguages());

                return c;
            });
        }
    }
}
