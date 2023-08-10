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

package it.smartcommunitylab.aac.base.service;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractSettingsMap;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.ConfigurableAuthorityService;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableProviderConverter;
import it.smartcommunitylab.aac.core.provider.config.ProviderConfig;
import it.smartcommunitylab.aac.core.provider.config.ProviderEntityConverter;
import it.smartcommunitylab.aac.core.service.ConfigurableProviderEntityService;
import it.smartcommunitylab.aac.core.service.ConfigurableProviderService;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;
import org.springframework.validation.SmartValidator;

@Transactional
public abstract class AbstractConfigurableProviderService<
    C extends ConfigurableProvider<S>, S extends AbstractSettingsMap
>
    implements ConfigurableProviderService<C>, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String type;

    protected final ConfigurableProviderEntityService providerService;
    protected final ConfigurableAuthorityService<? extends ConfigurableProviderAuthority<?, ? extends ProviderConfig<S, ?>, S, ?>, S> authorityService;

    // keep a local map for system providers since these are not in db
    // key is providerId
    protected final Map<String, C> systemProviders = new HashMap<>();

    protected SmartValidator validator;

    protected Converter<ConfigurableProvider<? extends ConfigMap>, ProviderEntity> configConverter;
    protected Converter<ProviderEntity, C> entityConverter;

    protected AbstractConfigurableProviderService(
        String type,
        ConfigurableAuthorityService<? extends ConfigurableProviderAuthority<?, ? extends ProviderConfig<S, ?>, S, ?>, S> providerAuthorityService,
        ConfigurableProviderEntityService providerService,
        Supplier<C> factory
    ) {
        Assert.hasText(type, "type is required");
        Assert.notNull(providerAuthorityService, "authority service is required");
        Assert.notNull(providerService, "provider entity service is required");

        this.type = type;
        this.providerService = providerService;
        this.authorityService = providerAuthorityService;

        //set default converters
        this.configConverter = new ConfigurableProviderConverter();
        this.entityConverter = new ProviderEntityConverter<>(factory);
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(configConverter, "config converter is required");
        Assert.notNull(entityConverter, "entity converter is required");
    }

    @Autowired
    public void setValidator(SmartValidator validator) {
        this.validator = validator;
    }

    public void setConfigConverter(
        Converter<ConfigurableProvider<? extends ConfigMap>, ProviderEntity> configConverter
    ) {
        this.configConverter = configConverter;
    }

    public void setEntityConverter(Converter<ProviderEntity, C> entityConverter) {
        this.entityConverter = entityConverter;
    }

    protected ConfigurationProvider<? extends ProviderConfig<S, ?>, S, ?> getConfigurationProvider(String authority)
        throws NoSuchAuthorityException {
        return authorityService.getAuthority(authority).getConfigurationProvider();
    }

    /*
     * CRUD via entities
     */
    @Transactional(readOnly = true)
    public Collection<C> listProviders(String realm) {
        logger.debug("list providers for realm {}", StringUtils.trimAllWhitespace(realm));

        if (SystemKeys.REALM_GLOBAL.equals(realm)) {
            // we do not persist in db global providers
            return Collections.emptyList();
        }

        if (SystemKeys.REALM_SYSTEM.equals(realm)) {
            return systemProviders.values();
        }

        List<ProviderEntity> providers = providerService.listProvidersByRealm(type, realm);
        return providers
            .stream()
            .map(p -> entityConverter.convert(p))
            .filter(c -> c != null)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public C findProvider(String providerId) {
        // lookup in global map first
        if (systemProviders.containsKey(providerId)) {
            return systemProviders.get(providerId);
        }

        ProviderEntity pe = providerService.findProvider(type, providerId);
        if (pe == null) {
            return null;
        }

        return entityConverter.convert(pe);
    }

    @Transactional(readOnly = true)
    public C getProvider(String providerId) throws NoSuchProviderException {
        logger.debug("get provider {}", StringUtils.trimAllWhitespace(providerId));

        // lookup in global map first
        if (systemProviders.containsKey(providerId)) {
            return systemProviders.get(providerId);
        }

        ProviderEntity pe = providerService.getProvider(type, providerId);
        C config = entityConverter.convert(pe);
        if (config == null) {
            throw new NoSuchProviderException();
        }

        return config;
    }

    public C addProvider(String realm, C cp) throws RegistrationException, SystemException, NoSuchAuthorityException {
        logger.debug("add provider for realm {}", StringUtils.trimAllWhitespace(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("provider bean: {}", StringUtils.trimAllWhitespace(cp.toString()));
        }

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        // check if id provided
        String providerId = cp.getProvider();
        if (StringUtils.hasText(providerId)) {
            ProviderEntity pe = providerService.findProvider(type, providerId);
            if (pe != null) {
                throw new RegistrationException("id already in use");
            }

            // validate
            if (providerId.length() < 3 || !Pattern.matches(SystemKeys.SLUG_PATTERN, providerId)) {
                throw new RegistrationException("invalid id");
            }
        }

        // set initial version to 1
        cp.setVersion(1);

        // unpack props and validate
        ProviderEntity entity = configConverter.convert(cp);
        if (entity == null) {
            throw new RegistrationException();
        }

        String authority = cp.getAuthority();

        // we validate config by converting to specific configMap
        ConfigurationProvider<?, ?, ?> configProvider = getConfigurationProvider(authority);
        ConfigMap config = configProvider.getConfigMap(cp.getConfiguration());
        ConfigMap settings = configProvider.getSettingsMap(cp.getSettings());

        // check with validator
        validateConfigMap(config);
        validateConfigMap(settings);

        //replace with valid configs
        entity.setConfigurationMap(config.getConfiguration());
        entity.setSettingsMap(settings.getConfiguration());

        if (logger.isTraceEnabled()) {
            logger.trace("entity bean: {}", StringUtils.trimAllWhitespace(entity.toString()));
            logger.trace("entity settings: {}", StringUtils.trimAllWhitespace(entity.getSettingsMap().toString()));
            logger.trace("entity config: {}", StringUtils.trimAllWhitespace(entity.getConfigurationMap().toString()));
        }

        ProviderEntity pe = providerService.saveProvider(type, providerId, entity);
        return entityConverter.convert(pe);
    }

    public C updateProvider(String providerId, C cp)
        throws NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("update provider {}", StringUtils.trimAllWhitespace(providerId));
        if (logger.isTraceEnabled()) {
            logger.trace("provider bean: {}", StringUtils.trimAllWhitespace(cp.toString()));
        }

        ProviderEntity pe = providerService.getProvider(type, providerId);

        if (StringUtils.hasText(cp.getProvider()) && !providerId.equals(cp.getProvider())) {
            throw new IllegalArgumentException("configuration does not match provider");
        }

        if (!pe.getAuthority().equals(cp.getAuthority())) {
            throw new IllegalArgumentException("authority mismatch");
        }

        if (!pe.getRealm().equals(cp.getRealm())) {
            throw new IllegalArgumentException("realm mismatch");
        }

        // check version and increment when necessary
        if (cp.getVersion() == null) {
            cp.setVersion(pe.getVersion());
        }

        if (cp.getVersion() < pe.getVersion()) {
            throw new IllegalArgumentException("invalid version");
        } else if (Objects.equals(cp.getVersion(), pe.getVersion())) {
            // increment
            cp.setVersion(pe.getVersion() + 1);
        }

        ProviderEntity entity = configConverter.convert(cp);
        if (entity == null) {
            throw new RegistrationException();
        }

        String authority = pe.getAuthority();

        ConfigurationProvider<?, ?, ?> configProvider = getConfigurationProvider(authority);
        ConfigMap config = configProvider.getConfigMap(cp.getConfiguration());
        ConfigMap settings = configProvider.getSettingsMap(cp.getSettings());

        // check with validator
        validateConfigMap(config);
        validateConfigMap(settings);

        //replace with valid configs
        entity.setConfigurationMap(config.getConfiguration());
        entity.setSettingsMap(settings.getConfiguration());

        if (logger.isTraceEnabled()) {
            logger.trace("entity bean: {}", StringUtils.trimAllWhitespace(entity.toString()));
            logger.trace("entity settings: {}", StringUtils.trimAllWhitespace(entity.getSettingsMap().toString()));
            logger.trace("entity config: {}", StringUtils.trimAllWhitespace(entity.getConfigurationMap().toString()));
        }

        ProviderEntity npe = providerService.saveProvider(type, providerId, entity);
        return entityConverter.convert(npe);
    }

    public void deleteProvider(String providerId) throws SystemException, NoSuchProviderException {
        logger.debug("delete provider {}", StringUtils.trimAllWhitespace(providerId));

        ProviderEntity pe = providerService.getProvider(type, providerId);
        providerService.deleteProvider(type, pe.getProvider());
    }

    /*
     * Validation
     */
    private void validateConfigMap(ConfigMap configurable) throws RegistrationException {
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
    }

    /*
     * Config via authorities
     */

    public void registerProvider(String providerId)
        throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        logger.debug("register provider {}", StringUtils.trimAllWhitespace(providerId));

        // fetch, only persisted configurations can be registered
        C cp = getProvider(providerId);

        //fetch config provider from authority
        ConfigurationProvider<?, S, ? extends ConfigMap> configProvider = getConfigurationProvider(cp.getAuthority());

        // always register and pop up errors
        configProvider.register(cp);
    }

    public void unregisterProvider(String providerId)
        throws NoSuchProviderException, SystemException, NoSuchAuthorityException {
        logger.debug("unregister provider {}", StringUtils.trimAllWhitespace(providerId));

        // fetch, only persisted configurations can be registered
        C cp = getProvider(providerId);

        //fetch config provider from authority
        ConfigurationProvider<?, S, ? extends ConfigMap> configProvider = getConfigurationProvider(cp.getAuthority());

        // always unregister, when not active nothing will happen
        configProvider.unregister(cp.getProvider());
    }

    public boolean isProviderRegistered(String providerId) throws NoSuchProviderException, NoSuchAuthorityException {
        // fetch, only persisted configurations can be registered
        C cp = getProvider(providerId);

        // ask authority
        return authorityService.getAuthority(cp.getAuthority()).hasProvider(cp.getProvider());
    }

    /*
     * Configuration schemas
     */

    // @Transactional(readOnly = true)
    // public ConfigurableProperties getConfigurableProperties(String authority) throws NoSuchAuthorityException {
    //     ConfigurationProvider<?, ?, ?> configProvider = getConfigurationProvider(authority);
    //     return configProvider.getDefaultConfigMap();
    // }

    @Transactional(readOnly = true)
    public JsonSchema getSettingsSchema(String authority) throws NoSuchAuthorityException {
        ConfigurationProvider<?, S, ? extends ConfigMap> configProvider = getConfigurationProvider(authority);
        try {
            return configProvider.getDefaultSettingsMap().getSchema();
        } catch (JsonMappingException e) {
            throw new SystemException(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public JsonSchema getConfigurationSchema(String authority) throws NoSuchAuthorityException {
        ConfigurationProvider<?, S, ? extends ConfigMap> configProvider = getConfigurationProvider(authority);
        try {
            return configProvider.getDefaultConfigMap().getSchema();
        } catch (JsonMappingException e) {
            throw new SystemException(e.getMessage());
        }
    }
}
