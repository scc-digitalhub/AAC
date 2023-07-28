/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.config.ProvidersProperties;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.persistence.IdentityProviderEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityProvider;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

@Service
@Transactional
public class IdentityProviderService
    extends ConfigurableProviderService<IdentityProviderAuthority<?, ?, ?, ?>, ConfigurableIdentityProvider, IdentityProviderEntity> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public IdentityProviderService(
        IdentityProviderAuthorityService authorityService,
        ConfigurableProviderEntityService<IdentityProviderEntity> providerService
    ) {
        super(authorityService, providerService);
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
            SystemKeys.AUTHORITY_INTERNAL,
            SystemKeys.AUTHORITY_INTERNAL,
            SystemKeys.REALM_SYSTEM
        );
        internalConfig.setVersion(1);
        logger.debug("configure internal idp for system realm");
        systemProviders.put(internalConfig.getProvider(), internalConfig);

        ConfigurableIdentityProvider internalPasswordIdpConfig = new ConfigurableIdentityProvider(
            SystemKeys.AUTHORITY_PASSWORD,
            SystemKeys.AUTHORITY_INTERNAL + "_" + SystemKeys.AUTHORITY_PASSWORD,
            SystemKeys.REALM_SYSTEM
        );
        internalPasswordIdpConfig.setVersion(1);
        logger.debug("configure internal password idp for system realm");
        systemProviders.put(internalPasswordIdpConfig.getProvider(), internalPasswordIdpConfig);
    }

    @Autowired
    private void setProviderProperties(ProvidersProperties providers) {
        // system providers
        if (providers != null) {
            // identity providers
            for (ConfigurableIdentityProvider cp : providers.getIdentity()) {
                if (cp == null || !cp.isEnabled()) {
                    continue;
                }
                String authority = cp.getAuthority();
                if (!StringUtils.hasText(authority)) {
                    continue;
                }

                // we handle only system providers, add others via bootstrap
                if (StringUtils.hasText(cp.getRealm()) && !SystemKeys.REALM_SYSTEM.equals(cp.getRealm())) {
                    logger.debug("skip provider {} for realm {}", cp.getAuthority(), String.valueOf(cp.getRealm()));
                    continue;
                }

                String providerId = StringUtils.hasText(cp.getProvider())
                    ? cp.getProvider()
                    : RandomStringUtils.randomAlphanumeric(10);
                logger.debug("configure provider {}:{} for realm system", authority, providerId);
                if (logger.isTraceEnabled()) {
                    logger.trace("provider config: {}", String.valueOf(cp.getConfiguration()));
                }

                try {
                    // we validate config by converting to specific configMap
                    ConfigurationProvider<?, ?, ?> configProvider = getConfigurationProvider(authority);
                    ConfigMap configurable = configProvider.getConfigMap(cp.getConfiguration());

                    // check with validator
                    if (validator != null) {
                        DataBinder binder = new DataBinder(configurable);
                        validator.validate(configurable, binder.getBindingResult());
                        if (binder.getBindingResult().hasErrors()) {
                            StringBuilder sb = new StringBuilder();
                            binder
                                .getBindingResult()
                                .getFieldErrors()
                                .forEach(e -> {
                                    sb.append(e.getField()).append(" ").append(e.getDefaultMessage());
                                });
                            String errorMsg = sb.toString();
                            throw new RegistrationException(errorMsg);
                        }
                    }

                    Map<String, Serializable> configuration = configurable.getConfiguration();

                    // build a new config to detach from props
                    ConfigurableIdentityProvider cip = new ConfigurableIdentityProvider(
                        authority,
                        providerId,
                        SystemKeys.REALM_SYSTEM
                    );
                    cip.setName(cp.getName());
                    cip.setTitleMap(cp.getTitleMap());
                    cip.setDescriptionMap(cip.getDescriptionMap());

                    cip.setLinkable(true);
                    cip.setPersistence(SystemKeys.PERSISTENCE_LEVEL_REPOSITORY);
                    cip.setEvents(SystemKeys.EVENTS_LEVEL_DETAILS);
                    cip.setPosition(cp.getPosition());

                    cip.setEnabled(true);
                    cip.setConfiguration(configuration);

                    // register
                    systemProviders.put(providerId, cip);
                } catch (
                    RegistrationException | SystemException | IllegalArgumentException | NoSuchAuthorityException ex
                ) {
                    logger.error("error configuring provider :" + ex.getMessage(), ex);
                }
            }
        }
    }

    static class IdentityProviderEntityConverter
        implements Converter<IdentityProviderEntity, ConfigurableIdentityProvider> {

        public ConfigurableIdentityProvider convert(IdentityProviderEntity pe) {
            ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider(
                pe.getAuthority(),
                pe.getProvider(),
                pe.getRealm()
            );
            cp.setConfiguration(pe.getConfigurationMap());
            cp.setVersion(pe.getVersion());

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

    class IdentityProviderConfigConverter implements Converter<ConfigurableIdentityProvider, IdentityProviderEntity> {

        @Override
        public IdentityProviderEntity convert(ConfigurableIdentityProvider reg) {
            IdentityProviderEntity pe = new IdentityProviderEntity();

            pe.setAuthority(reg.getAuthority());
            pe.setProvider(reg.getProvider());
            pe.setRealm(reg.getRealm());
            pe.setEnabled(reg.isEnabled());
            pe.setLinkable(reg.getLinkable());

            String name = reg.getName();
            if (StringUtils.hasText(name)) {
                name = Jsoup.clean(name, Safelist.none());
            }
            pe.setName(name);

            Map<String, String> titleMap = null;
            if (reg.getTitleMap() != null) {
                // cleanup every field via safelist
                titleMap =
                    reg
                        .getTitleMap()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> Map.entry(e.getKey(), Jsoup.clean(e.getValue(), Safelist.none())))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            }
            pe.setTitleMap(titleMap);

            Map<String, String> descriptionMap = null;
            if (reg.getDescriptionMap() != null) {
                // cleanup every field via safelist
                descriptionMap =
                    reg
                        .getDescriptionMap()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> Map.entry(e.getKey(), Jsoup.clean(e.getValue(), Safelist.none())))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            }
            pe.setDescriptionMap(descriptionMap);

            // TODO add enum
            String persistence = reg.getPersistence();
            String events = reg.getEvents();

            Integer position = (reg.getPosition() != null && reg.getPosition().intValue() > 0)
                ? reg.getPosition()
                : null;

            pe.setPersistence(persistence);
            pe.setEvents(events);
            pe.setPosition(position);

            pe.setConfigurationMap(reg.getConfiguration());
            pe.setVersion(reg.getVersion());

            pe.setHookFunctions(reg.getHookFunctions());

            return pe;
        }
    }
}
