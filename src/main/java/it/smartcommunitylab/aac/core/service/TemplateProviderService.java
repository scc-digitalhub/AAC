package it.smartcommunitylab.aac.core.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.persistence.TemplateProviderEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
@Transactional
public class TemplateProviderService
        extends ConfigurableProviderService<ConfigurableTemplateProvider, TemplateProviderEntity> {

    private TemplateProviderAuthorityService authorityService;

    public TemplateProviderService(ConfigurableProviderEntityService<TemplateProviderEntity> providerService) {
        super(providerService);

        // set converters
        setConfigConverter(new TemplateProviderConfigConverter());
        setEntityConverter(new TemplateProviderEntityConverter());

    }

    @Autowired
    public void setAuthorityService(TemplateProviderAuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Override
    protected ConfigurationProvider<?, ?, ?> getConfigurationProvider(String authority)
            throws NoSuchAuthorityException {
        return authorityService.getAuthority(authority).getConfigurationProvider();
    }

    class TemplateProviderConfigConverter implements Converter<ConfigurableTemplateProvider, TemplateProviderEntity> {

        @Override
        public TemplateProviderEntity convert(ConfigurableTemplateProvider reg) {
            TemplateProviderEntity pe = new TemplateProviderEntity();

            pe.setAuthority(reg.getAuthority());
            pe.setProvider(reg.getProvider());
            pe.setRealm(reg.getRealm());

            String name = reg.getName();
            String description = reg.getDescription();
            if (StringUtils.hasText(name)) {
                name = Jsoup.clean(name, Safelist.none());
            }
            if (StringUtils.hasText(description)) {
                description = Jsoup.clean(description, Safelist.none());
            }

            pe.setName(name);
            pe.setDescription(description);

            pe.setLanguages(StringUtils.collectionToCommaDelimitedString(reg.getLanguages()));

            pe.setEnabled(reg.isEnabled());
            pe.setConfigurationMap(reg.getConfiguration());

            return pe;
        }

    }

    class TemplateProviderEntityConverter implements Converter<TemplateProviderEntity, ConfigurableTemplateProvider> {

        @Override
        public ConfigurableTemplateProvider convert(TemplateProviderEntity pe) {
            ConfigurableTemplateProvider cp = new ConfigurableTemplateProvider(pe.getAuthority(), pe.getProvider(),
                    pe.getRealm());

            cp.setName(pe.getName());
            cp.setDescription(pe.getDescription());

            cp.setLanguages(StringUtils.commaDelimitedListToSet(pe.getLanguages()));

            cp.setEnabled(pe.isEnabled());
            cp.setConfiguration(pe.getConfigurationMap());

            return cp;
        }
    }
}
