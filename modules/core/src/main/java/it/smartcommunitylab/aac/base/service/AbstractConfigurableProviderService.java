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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.provider.config.AbstractConfigurableProviderConverter;
import it.smartcommunitylab.aac.base.provider.config.DefaultConfigMapConverter;
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
import it.smartcommunitylab.aac.core.provider.ResolvableGenericsTypeProvider;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableConverterFactory;
import it.smartcommunitylab.aac.core.service.ConfigurableProviderEntityService;
import it.smartcommunitylab.aac.core.service.ConfigurableProviderService;
import it.smartcommunitylab.aac.core.service.ConfigurableResourceProviderRegistry;
import it.smartcommunitylab.aac.model.ConfigMap;
import it.smartcommunitylab.aac.model.Resource;
import java.io.Serializable;
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

@Slf4j
public abstract class AbstractConfigurableProviderService<
    T extends ConfigurableResourceProvider<? extends Resource, P, S, ? extends ConfigMap>,
    C extends ConfigurableProvider<S>,
    P extends ProviderConfig<S, ? extends ConfigMap>,
    S extends ConfigMap
>
    implements ConfigurableProviderService<C>, ConfigurableResourceProviderRegistry<T, C, S>, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ResolvableType ctype;
    private final ResolvableType btype;

    protected final String type;

    protected final Map<String, ConfigurableProviderAuthority<T, P, S, ? extends ConfigMap>> authorities =
        new HashMap<>();

    // <? extends ConfigurableProviderAuthority<?, C, ? extends ProviderConfig<S, ?>, S, ? extends ConfigMap>
    // keep a local map for system providers since these are not in db
    // key is providerId
    protected final Map<String, C> systemProviders = new HashMap<>();

    protected SmartValidator validator;
    protected ConfigurableProviderEntityService providerService;

    protected final Map<String, Converter<C, P>> providerConfigConverters = new HashMap<>();
    protected final Map<String, Converter<Map<String, Serializable>, ? extends ConfigMap>> mapConfigConverters =
        new HashMap<>();

    protected final Converter<ConfigurableProvider<? extends ConfigMap>, ProviderEntity> configConverter;
    protected final Converter<ProviderEntity, C> entityConverter;
    protected final Converter<Map<String, Serializable>, S> settingsMapConverter;
    protected final ConfigurableConverterFactory configurableConverterFactory = ConfigurableConverterFactory.instance();

    //default configs
    protected S defaultSettings;
    protected final Map<String, Map<String, Serializable>> defaultConfigs = new HashMap<>();

    protected AbstractConfigurableProviderService() {
        log.debug("create provider for {}", getClass().getName());

        //extract type as type info
        ctype = ResolvableType.forClass(getClass());
        btype = ResolvableType.forClass(AbstractConfigurableProviderService.class, getClass());
        @SuppressWarnings("unchecked")
        Class<C> clazz = (Class<C>) btype.getGeneric(1).resolve();
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
        this.settingsMapConverter = new DefaultConfigMapConverter<>();

        //build default settings as blank
        try {
            this.defaultSettings = ((Class<S>) btype.getGeneric(3).resolve()).getDeclaredConstructor().newInstance();
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
    }

    @Autowired
    public void setProviderService(ConfigurableProviderEntityService providerService) {
        Assert.notNull(providerService, "provider service can not be null");
        this.providerService = providerService;
    }

    @Autowired
    public void setAuthorities(List<ConfigurableProviderAuthority<T, P, S, ? extends ConfigMap>> authorities) {
        Assert.notNull(authorities, "authorities are required");

        this.authorities.clear();

        authorities
            .stream()
            .forEach(a -> {
                this.authorities.put(a.getAuthorityId(), a);

                //extract configMap type and build default converters
                try {
                    if (a instanceof ResolvableGenericsTypeProvider) {
                        //resolve types and build default converter
                        @SuppressWarnings("unchecked")
                        Class<C> c = (Class<C>) btype.getGeneric(1).resolve();
                        Class<P> p = (Class<P>) ((ResolvableGenericsTypeProvider) a).getResolvableType(1).resolve();
                        Class<S> s = (Class<S>) btype.getGeneric(3).resolve();
                        Class<? extends ConfigMap> m = (Class<
                                ? extends ConfigMap
                            >) ((ResolvableGenericsTypeProvider) a).getResolvableType(3).resolve();

                        DefaultConfigMapConverter<? extends ConfigMap> mConv = ConfigurableConverterFactory.instance()
                            .buildConfigMapConverter(m);
                        this.mapConfigConverters.put(a.getAuthorityId(), mConv);

                        AbstractConfigurableProviderConverter<ProviderConfig<S, ConfigMap>, C, S, ConfigMap> pConv =
                            configurableConverterFactory.buildConfigurableProviderConverter(p, c, s, m);
                        this.providerConfigConverters.put(a.getAuthorityId(), (Converter<C, P>) pConv);
                    }
                } catch (Exception e) {
                    log.error("error building default converter: {}", e.getMessage());
                }
            });

        log.debug("registered authorities for {}: {}", type, this.authorities.keySet());
    }

    @Autowired
    public void setValidator(SmartValidator validator) {
        this.validator = validator;
    }

    public void registerProviderConfigConverter(String authority, Converter<C, P> converter) {
        Assert.hasText(authority, "authority can not be null or empty");
        Assert.notNull(converter, "config converter is required");

        this.providerConfigConverters.put(authority, converter);
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(providerService, "provider service is required");
        Assert.notNull(configConverter, "config converter is required");
        Assert.notNull(entityConverter, "entity converter is required");
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

        // we validate settings by converting to specific configMap
        ConfigMap settings = settingsMapConverter.convert(cp.getSettings());
        // check with validator
        validateConfigMap(settings);
        //replace with valid configs
        entity.setSettingsMap(settings.getConfiguration());

        // we validate config by converting to specific configMap
        Converter<Map<String, Serializable>, ? extends ConfigMap> configMapConverter = mapConfigConverters.get(
            authority
        );
        if (configMapConverter != null) {
            ConfigMap config = configMapConverter.convert(cp.getConfiguration());
            // check with validator
            validateConfigMap(config);
            //replace with valid configs
            entity.setConfigurationMap(config.getConfiguration());
        } else {
            log.warn("skip configMap validation for {}: missing converter for {}", providerId, authority);
        }

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

        // we validate settings by converting to specific configMap
        ConfigMap settings = settingsMapConverter.convert(cp.getSettings());
        // check with validator
        validateConfigMap(settings);
        //replace with valid configs
        entity.setSettingsMap(settings.getConfiguration());

        // we validate config by converting to specific configMap
        Converter<Map<String, Serializable>, ? extends ConfigMap> configMapConverter = mapConfigConverters.get(
            authority
        );
        if (configMapConverter != null) {
            ConfigMap config = configMapConverter.convert(cp.getConfiguration());
            // check with validator
            validateConfigMap(config);
            //replace with valid configs
            entity.setConfigurationMap(config.getConfiguration());
        } else {
            log.warn("skip configMap validation for {}: missing converter for {}", providerId, authority);
        }

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

    //  @Override
    //  public C registerProvider(P cp) throws IllegalArgumentException, RegistrationException, SystemException {
    //      // we support only matching provider as resource providers
    //      if (cp != null && getAuthorityId().equals(cp.getAuthority())) {
    //          String providerId = cp.getProvider();
    //          String realm = cp.getRealm();

    //          logger.debug("register provider {} for realm {}", providerId, realm);
    //          if (logger.isTraceEnabled()) {
    //              logger.trace("provider config: {}", String.valueOf(cp.getConfiguration()));
    //          }

    //          try {
    //              // check if exists or id clashes with another provider from a different realm
    //              C e = registrationRepository.findByProviderId(providerId);
    //              if (e != null) {
    //                  if (!realm.equals(e.getRealm())) {
    //                      // name clash
    //                      throw new RegistrationException(
    //                          "a provider with the same id already exists under a different realm"
    //                      );
    //                  }

    //                  // evaluate version against current
    //                  if (cp.getVersion() == null) {
    //                      throw new RegistrationException("invalid version");
    //                  } else if (e.getVersion() == cp.getVersion()) {
    //                      // same version, already registered, nothing to do
    //                      // load to warm cache
    //                      T rp = providers.get(providerId);

    //                      // return effective config
    //                      return rp.getConfig();
    //                  } else if (e.getVersion() > cp.getVersion()) {
    //                      throw new RegistrationException("invalid version");
    //                  }
    //              }

    //              // build config
    //              C providerConfig = getConfigurationProvider().getConfig(cp);
    //              if (logger.isTraceEnabled()) {
    //                  logger.trace(
    //                      "provider active config v{}: {}",
    //                      providerConfig.getVersion(),
    //                      String.valueOf(providerConfig.getConfigMap().getConfiguration())
    //                  );
    //              }

    //              //validate configs
    //              validateConfigMap(providerConfig.getSettingsMap());
    //              validateConfigMap(providerConfig.getConfigMap());

    //              // register, we defer loading
    //              // should update if existing
    //              registrationRepository.addRegistration(providerConfig);

    //              // load to warm cache
    //              T rp = providers.get(providerId);

    //              // return effective config
    //              return rp.getConfig();
    //          } catch (Exception ex) {
    //              // cleanup
    //              registrationRepository.removeRegistration(providerId);
    //              logger.error("error registering provider {}: {}", providerId, ex.getMessage());

    //              throw new RegistrationException("invalid provider configuration: " + ex.getMessage(), ex);
    //          }
    //      } else {
    //          throw new IllegalArgumentException("illegal configurable");
    //      }
    //  }

    //  @Override
    //  public void unregisterProvider(String providerId) {
    //      C registration = registrationRepository.findByProviderId(providerId);

    //      if (registration != null) {
    //          // can't unregister system providers, check
    //          if (SystemKeys.REALM_SYSTEM.equals(registration.getRealm())) {
    //              return;
    //          }

    //          logger.debug("unregister provider {} for realm {}", providerId, registration.getRealm());

    //          // remove from cache
    //          providers.invalidate(providerId);

    //          // remove from registrations
    //          registrationRepository.removeRegistration(providerId);
    //      }
    //  }

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

        ConfigurableProviderAuthority<T, P, S, ? extends ConfigMap> authority = authorities.get(cp.getAuthority());
        if (authority == null) {
            throw new NoSuchAuthorityException();
        }

        //convert
        Converter<C, P> converter = providerConfigConverters.get(authority.getAuthorityId());
        if (converter == null) {
            log.error("error registering provider {}: missing provider config converter for {}", providerId, authority);
            throw new IllegalArgumentException();
        }

        // always register and pop up errors
        authority.registerProvider(converter.convert(config));
    }

    public void unregisterProvider(String providerId)
        throws NoSuchProviderException, SystemException, NoSuchAuthorityException {
        logger.debug("unregister provider {}", StringUtils.trimAllWhitespace(providerId));

        // fetch, only persisted configurations can be registered
        C cp = getConfigurableProvider(providerId);

        ConfigurableProviderAuthority<T, P, S, ? extends ConfigMap> authority = authorities.get(cp.getAuthority());
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
    public T getResourceProvider(String providerId) throws NoSuchProviderException, NoSuchAuthorityException {
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

    // @Transactional(readOnly = true)
    // public ConfigurableProperties getConfigurableProperties(String authority, String type)
    //     throws NoSuchAuthorityException {
    //     ConfigurationProvider<?, ?, ?, ?> configProvider = getConfigurationProvider(authority);
    //     if (SystemKeys.RESOURCE_SETTINGS.equals(type)) {
    //         return configProvider.getDefaultSettingsMap();
    //     } else if (SystemKeys.RESOURCE_CONFIG.equals(type)) {
    //         return configProvider.getDefaultConfigMap();
    //     }
    //     throw new IllegalArgumentException("invalid type");
    // }

    @Transactional(readOnly = true)
    public JsonNode getSettingsSchema(String authority) throws NoSuchAuthorityException {
        //all authorities share the same settings map
        if (defaultSettings == null) {
            throw new NoSuchAuthorityException();
        }

        try {
            return defaultSettings.getSchema();
        } catch (JsonMappingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public JsonNode getConfigurationSchema(String authority) throws NoSuchAuthorityException {
        Converter<Map<String, Serializable>, ? extends ConfigMap> converter = mapConfigConverters.get(authority);
        if (converter == null) {
            throw new NoSuchAuthorityException();
        }

        Map<String, Serializable> values = defaultConfigs.get(authority) != null
            ? defaultConfigs.get(authority)
            : Map.of();

        //TODO describe with default values from map
        ConfigMap config = converter.convert(values);
        if (config == null) {
            throw new NoSuchAuthorityException();
        }

        try {
            return config.getSchema();
        } catch (JsonMappingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
