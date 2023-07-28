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

package it.smartcommunitylab.aac.controller;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import io.swagger.v3.oas.annotations.Operation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.AttributeProviderManager;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableAttributeProvider;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/*
 * Base controller for attribute providers
 */

@PreAuthorize("hasAuthority(this.authority)")
public class BaseAttributeProviderController implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected AttributeProviderManager providerManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(providerManager, "provider manager is required");
    }

    @Autowired
    public void setProviderManager(AttributeProviderManager providerManager) {
        this.providerManager = providerManager;
    }

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Attribute providers
     *
     * Manage only realm providers, with config stored
     */

    @GetMapping("/aps/{realm}")
    @Operation(summary = "list attribute providers from a given realm")
    public Collection<ConfigurableAttributeProvider> listAps(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm
    ) throws NoSuchRealmException {
        logger.debug("list ap for realm {}", StringUtils.trimAllWhitespace(realm));

        return providerManager
            .listProviders(realm)
            .stream()
            .map(cp -> {
                cp.setRegistered(providerManager.isProviderRegistered(realm, cp));
                return cp;
            })
            .collect(Collectors.toList());
    }

    @GetMapping("/aps/{realm}/{providerId}")
    @Operation(summary = "get a specific attribute provider from a given realm")
    public ConfigurableAttributeProvider getAp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId
    ) throws NoSuchProviderException, NoSuchRealmException, NoSuchAuthorityException {
        logger.debug(
            "get ap {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );

        ConfigurableAttributeProvider provider = providerManager.getProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;
    }

    @PostMapping("/aps/{realm}")
    @Operation(summary = "add a new attribute provider to a given realm")
    public ConfigurableAttributeProvider addAp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestBody @Valid @NotNull ConfigurableAttributeProvider reg
    ) throws NoSuchRealmException, NoSuchAuthorityException, RegistrationException, NoSuchProviderException {
        logger.debug("add ap to realm {}", StringUtils.trimAllWhitespace(realm));

        // enforce realm match
        reg.setRealm(realm);

        if (logger.isTraceEnabled()) {
            logger.trace("ap bean: {}", StringUtils.trimAllWhitespace(reg.toString()));
        }

        return providerManager.addProvider(realm, reg);
    }

    @PutMapping("/aps/{realm}/{providerId}")
    @Operation(summary = "update a specific attribute provider in a given realm")
    public ConfigurableAttributeProvider updateAp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        @RequestBody @Valid @NotNull ConfigurableAttributeProvider reg,
        @RequestParam(required = false, defaultValue = "false") Optional<Boolean> force
    ) throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug(
            "update ap {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );

        // enforce realm match
        reg.setRealm(realm);

        // check if active
        ConfigurableAttributeProvider provider = providerManager.getProvider(realm, providerId);

        // if force disable provider
        boolean forceRegistration = force.orElse(false);
        if (forceRegistration && providerManager.isProviderRegistered(realm, provider)) {
            provider = providerManager.unregisterProvider(realm, providerId);
        }

        if (logger.isTraceEnabled()) {
            logger.trace("ap bean: {}", StringUtils.trimAllWhitespace(reg.toString()));
        }

        provider = providerManager.updateProvider(realm, providerId, reg);

        // if force and enabled try to register
        if (forceRegistration && provider.isEnabled()) {
            try {
                provider = providerManager.registerProvider(realm, providerId);
            } catch (Exception e) {
                // ignore
            }
        }

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;
    }

    @DeleteMapping("/aps/{realm}/{providerId}")
    @Operation(summary = "delete a specific attribute provider from a given realm")
    public void deleteAp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId
    )
        throws NoSuchProviderException, NoSuchRealmException, SystemException, NoSuchAuthorityException, RegistrationException {
        logger.debug(
            "delete ap {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );

        providerManager.deleteProvider(realm, providerId);
    }

    /*
     * Registration with authorities
     */

    @PutMapping("/aps/{realm}/{providerId}/status")
    @Operation(summary = "activate a specific attribute provider from a given realm")
    public ConfigurableAttributeProvider registerAp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId
    )
        throws NoSuchProviderException, NoSuchRealmException, SystemException, NoSuchAuthorityException, RegistrationException {
        logger.debug(
            "register ap {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );

        ConfigurableAttributeProvider provider = providerManager.getProvider(realm, providerId);
        provider = providerManager.registerProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;
    }

    @DeleteMapping("/aps/{realm}/{providerId}/status")
    @Operation(summary = "deactivate a specific attribute provider from a given realm")
    public ConfigurableAttributeProvider unregisterAp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId
    )
        throws NoSuchProviderException, NoSuchRealmException, SystemException, NoSuchAuthorityException, RegistrationException {
        logger.debug(
            "unregister ap {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );

        ConfigurableAttributeProvider provider = providerManager.getProvider(realm, providerId);
        provider = providerManager.unregisterProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;
    }

    /*
     * Configuration schema
     */
    @GetMapping("/aps/{realm}/{providerId}/schema")
    @Operation(summary = "get an attribute provider configuration schema")
    public JsonSchema getApConfigurationSchema(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId
    ) throws NoSuchProviderException, NoSuchRealmException, NoSuchAuthorityException {
        logger.debug(
            "get ap config schema for {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );

        ConfigurableAttributeProvider provider = providerManager.getProvider(realm, providerId);
        return providerManager.getConfigurationSchema(realm, provider.getAuthority());
    }
}
