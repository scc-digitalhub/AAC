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

package it.smartcommunitylab.aac.base.provider;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.model.AbstractSettingsMap;
import it.smartcommunitylab.aac.base.provider.config.AbstractProviderConfig;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.DataBinder;
import org.springframework.validation.SmartValidator;

public abstract class AbstractConfigurationProvider<
    P extends AbstractProviderConfig<S, M>,
    C extends ConfigurableProvider<S>,
    S extends AbstractSettingsMap,
    M extends AbstractConfigMap
>
    implements ConfigurationProvider<P, C, S, M> {

    protected static final ObjectMapper mapper = new ObjectMapper()
        .addMixIn(AbstractSettingsMap.class, NoTypes.class)
        .addMixIn(AbstractConfigMap.class, NoTypes.class);
    private final JavaType configType;
    private final JavaType settingsType;

    private static final TypeReference<HashMap<String, Serializable>> typeRef =
        new TypeReference<HashMap<String, Serializable>>() {};

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String authority;
    protected final ProviderConfigRepository<P> registrationRepository;

    protected SmartValidator validator;

    protected S defaultSettingsMap;
    protected M defaultConfigMap;

    protected AbstractConfigurationProvider(String authority, ProviderConfigRepository<P> registrationRepository) {
        Assert.hasText(authority, "authority id  is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");

        this.authority = authority;
        this.registrationRepository = registrationRepository;

        this.settingsType = extractSettingsType();
        this.configType = extractConfigType();

        Assert.notNull(settingsType, "settings type could not be extracted");
        Assert.notNull(configType, "config type could not be extracted");
    }

    protected JavaType _extractJavaType(int pos) {
        // resolve generics type via subclass trick
        Type t = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[pos];
        return mapper.getTypeFactory().constructSimpleType((Class<?>) t, null);
    }

    protected JavaType extractSettingsType() {
        return _extractJavaType(2);
    }

    protected JavaType extractConfigType() {
        return _extractJavaType(3);
    }

    protected void setDefaultSettingsMap(S defaultSettingsMap) {
        this.defaultSettingsMap = defaultSettingsMap;
    }

    protected void setDefaultConfigMap(M defaultConfigMap) {
        this.defaultConfigMap = defaultConfigMap;
    }

    @Autowired
    public void setValidator(SmartValidator validator) {
        this.validator = validator;
    }

    protected abstract C buildConfigurable(P pc);

    protected abstract P buildConfig(C cp);

    // protected C buildConfig(C cp) {
    //     try {
    //         Type tc = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    //         Type ts = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    //         Type tm = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[2];

    //         C c = (C) tc
    //             .getClass()
    //             .getDeclaredConstructor(String.class, (Class<?>) ts, (Class<?>) tm)
    //             .newInstance(cp.getSettings(), cp.getConfiguration());
    //         return c;
    //     } catch (Exception e) {
    //         return null;
    //     }
    // }

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public ProviderConfigRepository<P> getRepository() {
        return this.registrationRepository;
    }

    @Override
    public P getConfig(C cp, boolean mergeDefault) {
        if (mergeDefault && defaultConfigMap != null) {
            // merge configMap with default on missing values
            Map<String, Serializable> map = new HashMap<>();
            map.putAll(cp.getConfiguration());

            Map<String, Serializable> defaultMap = defaultConfigMap.getConfiguration();
            defaultMap
                .entrySet()
                .forEach(e -> {
                    map.putIfAbsent(e.getKey(), e.getValue());
                });

            cp.setConfiguration(map);
        }

        if (mergeDefault && defaultSettingsMap != null) {
            // merge settingsMap with default on missing values
            Map<String, Serializable> map = new HashMap<>();
            map.putAll(cp.getSettings());

            Map<String, Serializable> defaultMap = defaultSettingsMap.getConfiguration();
            defaultMap
                .entrySet()
                .forEach(e -> {
                    map.putIfAbsent(e.getKey(), e.getValue());
                });

            cp.setSettings(map);
        }

        return buildConfig(cp);
    }

    @Override
    public P getConfig(C cp) {
        return getConfig(cp, true);
    }

    @Override
    public C getConfigurable(P providerConfig) {
        return buildConfigurable(providerConfig);
    }

    @Override
    public S getDefaultSettingsMap() {
        return defaultSettingsMap;
    }

    @Override
    public M getDefaultConfigMap() {
        return defaultConfigMap;
    }

    @Override
    public M getConfigMap(Map<String, Serializable> map) {
        // return a valid config from props
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        M m = mapper.convertValue(map, configType);
        return m;
    }

    @Override
    public S getSettingsMap(Map<String, Serializable> map) {
        // return a valid config from props
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        S m = mapper.convertValue(map, settingsType);
        return m;
    }

    protected Map<String, Serializable> getConfiguration(ConfigMap configMap) {
        if (configMap == null) {
            return Collections.emptyMap();
        }

        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(configMap, typeRef);
    }

    // @Override
    // public JsonSchema getSchema() {
    //     try {
    //         return defaultConfigMap.getSchema();
    //     } catch (JsonMappingException e) {
    //         return null;
    //     }
    // }

    @Override
    public P register(C cp) throws RegistrationException {
        // we support only matching provider as resource providers
        if (cp != null && getAuthority().equals(cp.getAuthority())) {
            String providerId = cp.getProvider();
            String realm = cp.getRealm();

            logger.debug("register provider {} for realm {}", providerId, realm);
            if (logger.isTraceEnabled()) {
                logger.trace("provider config: {}", String.valueOf(cp.getConfiguration()));
            }

            try {
                // check if exists or id clashes with another provider from a different realm
                P e = registrationRepository.findByProviderId(providerId);
                if (e != null) {
                    if (!realm.equals(e.getRealm())) {
                        // name clash
                        throw new RegistrationException(
                            "a provider with the same id already exists under a different realm"
                        );
                    }

                    // evaluate version against current
                    if (cp.getVersion() == null) {
                        throw new RegistrationException("invalid version");
                    } else if (e.getVersion() == cp.getVersion()) {
                        return e;
                    } else if (e.getVersion() > cp.getVersion()) {
                        throw new RegistrationException("invalid version");
                    }
                }

                // build config
                P providerConfig = getConfig(cp);
                if (logger.isTraceEnabled()) {
                    logger.trace(
                        "provider active config v{}: {}",
                        providerConfig.getVersion(),
                        String.valueOf(providerConfig.getConfigMap().getConfiguration())
                    );
                }

                //validate configs
                validateConfigMap(providerConfig.getSettingsMap());
                validateConfigMap(providerConfig.getConfigMap());

                // register, we defer loading to authority
                // should update if existing
                registrationRepository.addRegistration(providerConfig);

                // return effective config from repo
                return registrationRepository.findByProviderId(providerId);
            } catch (Exception ex) {
                // cleanup
                registrationRepository.removeRegistration(providerId);
                logger.error("error registering provider {}: {}", providerId, ex.getMessage());

                throw new RegistrationException("invalid provider configuration: " + ex.getMessage(), ex);
            }
        } else {
            throw new IllegalArgumentException("illegal configurable");
        }
    }

    @Override
    public void unregister(String providerId) {
        P registration = registrationRepository.findByProviderId(providerId);

        if (registration != null) {
            // can't unregister system providers, check
            if (SystemKeys.REALM_SYSTEM.equals(registration.getRealm())) {
                return;
            }

            logger.debug("unregister provider {} for realm {}", providerId, registration.getRealm());

            // remove from registrations, we defer unloading to authority
            registrationRepository.removeRegistration(providerId);
        }
    }

    protected void validateConfigMap(ConfigMap configurable) throws RegistrationException {
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    static class NoTypes {}
}
