package it.smartcommunitylab.aac.core.service;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

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
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.config.ProvidersProperties;
import it.smartcommunitylab.aac.config.ProvidersProperties.ProviderConfiguration;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.persistence.IdentityProviderEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
@Transactional
public class IdentityProviderService
        extends ConfigurableProviderService<ConfigurableIdentityProvider, IdentityProviderEntity> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private IdentityProviderAuthorityService authorityService;

    public IdentityProviderService(ConfigurableProviderEntityService<IdentityProviderEntity> providerService,
            IdentityAuthoritiesProperties authoritiesProperties, ProvidersProperties providers) {
        super(providerService);

        // set converters
        this.setConfigConverter(new IdentityProviderConfigConverter());
        this.setEntityConverter(new IdentityProviderEntityConverter());

        // create system idps
        // these users access administrative contexts, they will have realm="system"
        // we expect no client/services in global+system realm!
        // note: we let registration with authorities to bootstrap

        // always configure internal + password idp for system, required by admin
        // account
        // TODO make configurable
        ConfigurableIdentityProvider internalConfig = new ConfigurableIdentityProvider(
                SystemKeys.AUTHORITY_INTERNAL, SystemKeys.AUTHORITY_INTERNAL,
                SystemKeys.REALM_SYSTEM);
        logger.debug("configure internal idp for system realm");
        systemProviders.put(internalConfig.getProvider(), internalConfig);

        ConfigurableIdentityProvider internalPasswordIdpConfig = new ConfigurableIdentityProvider(
                SystemKeys.AUTHORITY_PASSWORD, SystemKeys.AUTHORITY_INTERNAL + "_" + SystemKeys.AUTHORITY_PASSWORD,
                SystemKeys.REALM_SYSTEM);
        logger.debug("configure internal password idp for system realm");
        systemProviders.put(internalPasswordIdpConfig.getProvider(), internalPasswordIdpConfig);

        // system providers
        if (providers != null) {
            // identity providers
            for (ProviderConfiguration providerConfig : providers.getIdentity()) {
                try {
                    // check match, we import only idps
                    if (!SystemKeys.RESOURCE_IDENTITY.equals(providerConfig.getType())) {
                        continue;
                    }

                    if (providerConfig.getEnable() != null && !providerConfig.isEnabled()) {
                        continue;
                    }

                    // we handle only system providers, add others via bootstrap
                    if (SystemKeys.REALM_SYSTEM.equals(providerConfig.getRealm())
                            || !StringUtils.hasText(providerConfig.getRealm())) {
                        logger.debug(
                                "configure provider for " + providerConfig.getType() + " for system realm: "
                                        + providerConfig.toString());

                        // translate config
                        ConfigurableIdentityProvider provider = new ConfigurableIdentityProvider(
                                providerConfig.getAuthority(),
                                providerConfig.getProvider(), SystemKeys.REALM_SYSTEM);
                        provider.setName(providerConfig.getName());
                        provider.setTitleMap(providerConfig.getTitle());
                        provider.setDescriptionMap(providerConfig.getDescription());
                        provider.setEnabled(true);
                        for (Map.Entry<String, String> entry : providerConfig.getConfiguration().entrySet()) {
                            provider.setConfigurationProperty(entry.getKey(), entry.getValue());
                        }

                        // by default global providers persist account + attributes
                        String persistenceLevel = SystemKeys.PERSISTENCE_LEVEL_REPOSITORY;
                        if (StringUtils.hasText(providerConfig.getPersistence())) {
                            // set persistence level
                            persistenceLevel = providerConfig.getPersistence();
                        }
                        provider.setPersistence(persistenceLevel);

                        // set default event level
                        String eventsLevel = SystemKeys.EVENTS_LEVEL_DETAILS;
                        if (StringUtils.hasText(providerConfig.getEvents())) {
                            // set persistence level
                            eventsLevel = providerConfig.getEvents();
                        }
                        provider.setEvents(eventsLevel);

                        // register
                        systemProviders.put(provider.getProvider(), provider);
                    }
                } catch (RegistrationException | SystemException | IllegalArgumentException ex) {
                    logger.error("error configuring provider :" + ex.getMessage(), ex);
                }
            }
        }
    }

    @Autowired
    public void setAuthorityService(IdentityProviderAuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Override
    protected ConfigurationProvider<?, ?, ?> getConfigurationProvider(String authority)
            throws NoSuchAuthorityException {
        return authorityService.getAuthority(authority).getConfigurationProvider();
    }

    static class IdentityProviderEntityConverter
            implements Converter<IdentityProviderEntity, ConfigurableIdentityProvider> {

        public ConfigurableIdentityProvider convert(IdentityProviderEntity pe) {
            ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider(pe.getAuthority(), pe.getProvider(),
                    pe.getRealm());
            cp.setConfiguration(pe.getConfigurationMap());
            cp.setEnabled(pe.isEnabled());
            cp.setPersistence(pe.getPersistence());
            cp.setEvents(pe.getEvents());
            cp.setPosition(pe.getPosition());
            cp.setHookFunctions(pe.getHookFunctions() != null ? pe.getHookFunctions() : Collections.emptyMap());

            cp.setName(pe.getName());
            cp.setTitleMap(pe.getTitleMap());
            cp.setDescriptionMap(pe.getDescriptionMap());

            return cp;
        }

    }

    class IdentityProviderConfigConverter
            implements Converter<ConfigurableIdentityProvider, IdentityProviderEntity> {

        @Override
        public IdentityProviderEntity convert(ConfigurableIdentityProvider reg) {
            IdentityProviderEntity pe = new IdentityProviderEntity();

            pe.setAuthority(reg.getAuthority());
            pe.setProvider(reg.getProvider());
            pe.setRealm(reg.getRealm());
            pe.setEnabled(reg.isEnabled());
            pe.setLinkable(reg.isLinkable());

            String name = reg.getName();
            if (StringUtils.hasText(name)) {
                name = Jsoup.clean(name, Safelist.none());
            }
            pe.setName(name);

            Map<String, String> titleMap = null;
            if (reg.getTitleMap() != null) {
                // cleanup every field via safelist
                titleMap = reg.getTitleMap().entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> Map.entry(e.getKey(), Jsoup.clean(e.getValue(), Safelist.none())))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            }
            pe.setTitleMap(titleMap);

            Map<String, String> descriptionMap = null;
            if (reg.getDescriptionMap() != null) {
                // cleanup every field via safelist
                descriptionMap = reg.getDescriptionMap().entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> Map.entry(e.getKey(), Jsoup.clean(e.getValue(), Safelist.none())))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            }
            pe.setDescriptionMap(descriptionMap);

            // TODO add enum
            String persistence = reg.getPersistence();
            if (!StringUtils.hasText(persistence)) {
                persistence = SystemKeys.PERSISTENCE_LEVEL_REPOSITORY;
            }

            if (!SystemKeys.PERSISTENCE_LEVEL_REPOSITORY.equals(persistence)
                    && !SystemKeys.PERSISTENCE_LEVEL_MEMORY.equals(persistence)
                    && !SystemKeys.PERSISTENCE_LEVEL_SESSION.equals(persistence)
                    && !SystemKeys.PERSISTENCE_LEVEL_NONE.equals(persistence)) {
                throw new RegistrationException("invalid persistence level");
            }

            String events = reg.getEvents();
            if (!StringUtils.hasText(events)) {
                events = SystemKeys.EVENTS_LEVEL_DETAILS;
            }

            if (!SystemKeys.EVENTS_LEVEL_DETAILS.equals(events)
                    && !SystemKeys.EVENTS_LEVEL_FULL.equals(events)
                    && !SystemKeys.EVENTS_LEVEL_MINIMAL.equals(events)
                    && !SystemKeys.EVENTS_LEVEL_NONE.equals(events)) {
                throw new RegistrationException("invalid events level");
            }

            Integer position = (reg.getPosition() != null && reg.getPosition().intValue() > 0) ? reg.getPosition()
                    : null;

            pe.setPersistence(persistence);
            pe.setEvents(events);
            pe.setPosition(position);

            pe.setConfigurationMap(reg.getConfiguration());
            pe.setHookFunctions(reg.getHookFunctions());

            return pe;
        }

    }

}
