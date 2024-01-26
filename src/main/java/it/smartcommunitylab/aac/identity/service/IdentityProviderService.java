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

package it.smartcommunitylab.aac.identity.service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.service.AbstractConfigurableProviderService;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.config.ProvidersProperties;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.service.ConfigurableProviderEntityService;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

@Service
@Transactional
public class IdentityProviderService
    extends AbstractConfigurableProviderService<ConfigurableIdentityProvider, IdentityProviderSettingsMap> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public IdentityProviderService(
        IdentityProviderAuthorityService authorityService,
        ConfigurableProviderEntityService providerService
    ) {
        super(SystemKeys.RESOURCE_IDENTITY, authorityService, providerService, ConfigurableIdentityProvider::new);
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
        internalConfig.setEnabled(true);
        logger.debug("configure internal idp for system realm");
        systemProviders.put(internalConfig.getProvider(), internalConfig);

        ConfigurableIdentityProvider internalPasswordIdpConfig = new ConfigurableIdentityProvider(
            SystemKeys.AUTHORITY_PASSWORD,
            SystemKeys.AUTHORITY_INTERNAL + "_" + SystemKeys.AUTHORITY_PASSWORD,
            SystemKeys.REALM_SYSTEM
        );
        internalPasswordIdpConfig.setVersion(1);
        internalPasswordIdpConfig.setEnabled(true);
        logger.debug("configure internal password idp for system realm");
        systemProviders.put(internalPasswordIdpConfig.getProvider(), internalPasswordIdpConfig);
    }

    @Autowired
    private void setProviderProperties(ProvidersProperties providers) {
        // system providers
        if (providers != null) {
            // identity providers
            for (ConfigurableIdentityProvider cp : providers.getIdentity()) {
                if (cp == null || SystemKeys.RESOURCE_IDENTITY.equals(cp.getType()) || !cp.isEnabled()) {
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
                    : UUID.randomUUID().toString();
                logger.debug("configure provider {}:{} for realm system", authority, providerId);
                if (logger.isTraceEnabled()) {
                    logger.trace("provider config: {}", String.valueOf(cp.getConfiguration()));
                }

                try {
                    // we validate config by converting to specific configMap
                    ConfigurationProvider<?, ?, ?, ?> configProvider = getConfigurationProvider(authority);
                    ConfigMap configurable = configProvider.getConfigMap(cp.getConfiguration());
                    ConfigMap settings = configProvider.getSettingsMap(cp.getSettings());

                    // check with validator
                    //TODO check settings?
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

                    cip.setEnabled(true);
                    cip.setSettings(settings.getConfiguration());
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

    @Override
    protected void validateConfigMap(ConfigMap configurable) throws RegistrationException {
        if (configurable instanceof IdentityProviderSettingsMap) {
            sanitizeIdentityProviderSettingsMap((IdentityProviderSettingsMap) configurable);
        }
        super.validateConfigMap(configurable);
    }

    private void sanitizeIdentityProviderSettingsMap(IdentityProviderSettingsMap map) {
        String notes = map.getNotes();
        if (!StringUtils.hasText(notes)) {
            return;
        }
        notes = Jsoup.clean(notes, Safelist.none());
        map.setNotes(notes);
    }
}
