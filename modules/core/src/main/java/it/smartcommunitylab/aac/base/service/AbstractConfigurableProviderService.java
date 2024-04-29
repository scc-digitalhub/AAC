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
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.core.persistence.ConfigurableProviderConverter;
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;
import it.smartcommunitylab.aac.core.persistence.ProviderEntityConverter;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.service.ConfigurableProviderEntityService;
import it.smartcommunitylab.aac.core.service.ConfigurableProviderService;
import it.smartcommunitylab.aac.core.service.ConfigurableResourceProviderRegistry;
import it.smartcommunitylab.aac.model.ConfigMap;
import it.smartcommunitylab.aac.model.ConfigurableProperties;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Transactional
@Slf4j
public abstract class AbstractConfigurableProviderService<
    C extends ConfigurableProvider<S>,
    S extends ConfigMap,
    T extends ConfigurableResourceProvider<?, ProviderConfig<S, ?>, S, ?>
>
    implements ConfigurableProviderService<C>, ConfigurableResourceProviderRegistry<T, C, S>, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String type;

    protected final ConfigurableProviderEntityService providerService;
    protected final Map<String, ConfigurableProviderAuthority<T, C, ProviderConfig<S, ?>, S, ?>> authorities =
        new HashMap<>();
    // <? extends ConfigurableProviderAuthority<?, C, ? extends ProviderConfig<S, ?>, S, ? extends ConfigMap>
    // keep a local map for system providers since these are not in db
    // key is providerId
    protected final Map<String, C> systemProviders = new HashMap<>();

    protected SmartValidator validator;

    protected Converter<ConfigurableProvider<? extends ConfigMap>, ProviderEntity> configConverter;
    protected Converter<ProviderEntity, C> entityConverter;

    protected AbstractConfigurableProviderService(ConfigurableProviderEntityService providerService) {
        Assert.notNull(providerService, "provider entity service is required");

        log.debug("create provider for {}", getClass().getName());
        this.providerService = providerService;

        //extract type as type info
        ResolvableType resolvableType = ResolvableType.forClass(getClass());
        @SuppressWarnings("unchecked")
        Class<C> clazz = (Class<C>) resolvableType.getSuperType().getGeneric(0).resolve();
        if (clazz == null) {
            throw new IllegalArgumentException("class is not resolvable");
        }
        this.type = clazz.getSimpleName();

        log.debug("configurable class resolved as type:{}", type);

        //build a default factory via no-args constructor
        Supplier<C> factory = () -> {
            try {
                return (clazz.getDeclaredConstructor().newInstance());
            } catch (
                InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e
            ) {
                throw new IllegalArgumentException();
            }
        };

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
    public void setAuthorities(List<ConfigurableProviderAuthority<T, C, ProviderConfig<S, ?>, S, ?>> authorities) {
        Assert.notNull(authorities, "authorities are required");

        this.authorities.clear();

        authorities
            .stream()
            .forEach(a -> {
                this.authorities.put(a.getAuthorityId(), a);
            });

        log.debug("registered authorities for {}: {}", type, this.authorities.keySet());
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

    public ConfigurationProvider<ProviderConfig<S, ?>, C, S, ?> getConfigurationProvider(String authorityId)
        throws NoSuchAuthorityException {
        //fetch config provider from authority
        ConfigurableProviderAuthority<T, C, ProviderConfig<S, ?>, S, ?> authority = authorities.get(authorityId);
        if (authority == null) {
            throw new NoSuchAuthorityException();
        }

        ConfigurationProvider<ProviderConfig<S, ?>, C, S, ?> configProvider = authority.getConfigurationProvider();
        //config provider could be null for authorities which don't expose dynamic config
        if (configProvider == null) {
            throw new IllegalArgumentException("config provider not available");
        }

        return configProvider;
    }

    /*
     * CRUD via entities
     */
    @Transactional(readOnly = true)
    public Page<C> searchConfigurableProviders(String realm, String q, Pageable pageRequest) {
        Page<ProviderEntity> page = providerService.searchProviders(type, realm, q, pageRequest);
        return PageableExecutionUtils.getPage(
            page.getContent().stream().map(pe -> entityConverter.convert(pe)).collect(Collectors.toList()),
            pageRequest,
            () -> page.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public Collection<C> listConfigurableProviders() {
        logger.debug("list all providers");

        //return only persisted
        List<ProviderEntity> providers = providerService.listProviders(type);
        return providers
            .stream()
            .map(p -> entityConverter.convert(p))
            .filter(c -> c != null)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Collection<C> listConfigurableProvidersByRealm(String realm) {
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
    public C findConfigurableProvider(String providerId) {
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
    public C getConfigurableProvider(String providerId) throws NoSuchProviderException {
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

    public C addConfigurableProvider(String realm, String providerId, C cp)
        throws RegistrationException, SystemException, NoSuchAuthorityException, MethodArgumentNotValidException {
        logger.debug("add provider for realm {}", StringUtils.trimAllWhitespace(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("provider bean: {}", StringUtils.trimAllWhitespace(cp.toString()));
        }

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        // check if id provided
        if (StringUtils.hasText(providerId)) {
            ProviderEntity pe = providerService.findProvider(type, providerId);
            if (pe != null) {
                throw new RegistrationException("id already in use");
            }

            // validate
            if (providerId.length() < 3 || !Pattern.matches(SystemKeys.SLUG_PATTERN, providerId)) {
                throw new RegistrationException("invalid id");
            }
        } else {
            //generate
            //note: we do not check for duplicates because UUIDs are safe enough
            providerId = UUID.randomUUID().toString();
        }

        //enforce matching providerId
        cp.setProvider(providerId);

        //check for duplicate names
        if (
            StringUtils.hasText(cp.getName()) &&
            providerService
                .listProvidersByRealm(type, realm)
                .stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(cp.getName()))
        ) {
            throw new AlreadyRegisteredException();
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
        ConfigurationProvider<?, ?, ?, ?> configProvider = getConfigurationProvider(authority);
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

    public C updateConfigurableProvider(String providerId, C cp)
        throws NoSuchProviderException, NoSuchAuthorityException, RegistrationException, MethodArgumentNotValidException {
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

        //check for duplicate names if name changes
        if (
            StringUtils.hasText(cp.getName()) &&
            !cp.getName().equals(pe.getName()) &&
            providerService
                .listProvidersByRealm(type, pe.getRealm())
                .stream()
                .filter(p -> !(p.getProvider().equals(providerId)))
                .anyMatch(p -> p.getName().equalsIgnoreCase(cp.getName()))
        ) {
            throw new AlreadyRegisteredException();
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

        ConfigurationProvider<?, ?, ?, ?> configProvider = getConfigurationProvider(authority);
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

    public void deleteConfigurableProvider(String providerId) throws SystemException, NoSuchProviderException {
        logger.debug("delete provider {}", StringUtils.trimAllWhitespace(providerId));

        ProviderEntity pe = providerService.getProvider(type, providerId);
        providerService.deleteProvider(type, pe.getProvider());
    }

    /*
     * Validation
     */
    public void validateConfigMap(ConfigMap configurable)
        throws RegistrationException, MethodArgumentNotValidException {
        // check with validator
        if (validator != null) {
            DataBinder binder = new DataBinder(configurable);
            validator.validate(configurable, binder.getBindingResult());
            if (binder.getBindingResult().hasErrors()) {
                // StringBuilder sb = new StringBuilder();
                // binder
                //     .getBindingResult()
                //     .getFieldErrors()
                //     .forEach(e -> {
                //         sb.append(e.getField()).append(" ").append(e.getDefaultMessage()).append(", ");
                //     });
                // String errorMsg = sb.toString();
                // throw new RegistrationException(errorMsg);
                MethodParameter methodParameter;
                try {
                    methodParameter = new MethodParameter(
                        this.getClass().getMethod("validateConfigMap", ConfigMap.class),
                        0
                    );
                    throw new MethodArgumentNotValidException(methodParameter, binder.getBindingResult());
                } catch (NoSuchMethodException | SecurityException ex) {
                    StringBuilder sb = new StringBuilder();
                    binder
                        .getBindingResult()
                        .getFieldErrors()
                        .forEach(e -> {
                            sb.append(e.getField()).append(" ").append(e.getDefaultMessage()).append(", ");
                        });
                    String errorMsg = sb.toString();
                    throw new RegistrationException(errorMsg);
                }
            }
        }
        //TODO check with config provider
    }

    /*
     * Config via authorities
     */

    public void registerProvider(String providerId, C cp)
        throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        Assert.hasText(providerId, "provider id can not be null or empty");
        Assert.notNull(cp, "configurable provider can not be null");
        Assert.isTrue(providerId.equals(cp.getProvider()), "providerId must match configuration");

        logger.debug("register provider {}", StringUtils.trimAllWhitespace(cp.getProvider()));

        // fetch, only persisted configurations can be registered
        C config = getConfigurableProvider(cp.getProvider());

        //TODO evaluate if config are the same
        if (cp.getAuthority() != config.getAuthority()) {
            throw new IllegalArgumentException();
        }

        ConfigurableProviderAuthority<T, C, ProviderConfig<S, ?>, S, ?> authority = authorities.get(cp.getAuthority());
        if (authority == null) {
            throw new NoSuchAuthorityException();
        }

        // always register and pop up errors
        authority.registerProvider(cp);
    }

    public void unregisterProvider(String providerId)
        throws NoSuchProviderException, SystemException, NoSuchAuthorityException {
        logger.debug("unregister provider {}", StringUtils.trimAllWhitespace(providerId));

        // fetch, only persisted configurations can be registered
        C cp = getConfigurableProvider(providerId);

        ConfigurableProviderAuthority<T, C, ProviderConfig<S, ?>, S, ?> authority = authorities.get(cp.getAuthority());
        if (authority == null) {
            throw new NoSuchAuthorityException();
        }

        // always unregister, when not active nothing will happen
        authority.unregisterProvider(cp.getProvider());
    }

    public boolean isProviderRegistered(String providerId) throws NoSuchProviderException, NoSuchAuthorityException {
        // fetch, only persisted configurations can be registered
        C cp = getConfigurableProvider(providerId);

        if (!cp.isEnabled()) {
            return false;
        }

        return hasResourceProvider(providerId);
    }

    @Override
    public boolean hasResourceProvider(String providerId) {
        // ask authority
        return authorities.values().stream().anyMatch(a -> a.hasProvider(providerId));
    }

    @Override
    public T findResourceProvider(String providerId) {
        // ask authority
        return authorities
            .values()
            .stream()
            .map(a -> a.findProvider(providerId))
            .filter(p -> p != null)
            .findFirst()
            .orElse(null);
    }

    @Override
    public T getResourceProvider(String providerId) throws NoSuchProviderException {
        // ask authority
        return authorities
            .values()
            .stream()
            .map(a -> a.findProvider(providerId))
            .filter(p -> p != null)
            .findFirst()
            .orElseThrow(() -> new NoSuchProviderException());
    }

    @Override
    public Collection<T> listResourceProviders() {
        // ask authority
        return authorities.values().stream().flatMap(a -> a.listProviders().stream()).collect(Collectors.toList());
    }

    @Override
    public Collection<T> listResourceProvidersByRealm(String realm) {
        // ask authority
        return authorities
            .values()
            .stream()
            .flatMap(a -> a.getProvidersByRealm(realm).stream())
            .collect(Collectors.toList());
    }

    /*
     * Configuration schemas
     */

    @Transactional(readOnly = true)
    public ConfigurableProperties getConfigurableProperties(String authority, String type)
        throws NoSuchAuthorityException {
        ConfigurationProvider<?, ?, ?, ?> configProvider = getConfigurationProvider(authority);
        if (SystemKeys.RESOURCE_SETTINGS.equals(type)) {
            return configProvider.getDefaultSettingsMap();
        } else if (SystemKeys.RESOURCE_CONFIG.equals(type)) {
            return configProvider.getDefaultConfigMap();
        }
        throw new IllegalArgumentException("invalid type");
    }

    @Transactional(readOnly = true)
    public JsonSchema getSettingsSchema(String authority) throws NoSuchAuthorityException {
        ConfigurationProvider<?, ?, S, ? extends ConfigMap> configProvider = getConfigurationProvider(authority);
        try {
            return configProvider.getDefaultSettingsMap().getSchema();
        } catch (JsonMappingException e) {
            throw new SystemException(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public JsonSchema getConfigurationSchema(String authority) throws NoSuchAuthorityException {
        ConfigurationProvider<?, ?, S, ? extends ConfigMap> configProvider = getConfigurationProvider(authority);
        try {
            return configProvider.getDefaultConfigMap().getSchema();
        } catch (JsonMappingException e) {
            throw new SystemException(e.getMessage());
        }
    }
}
