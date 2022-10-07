package it.smartcommunitylab.aac.core.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Transactional
public abstract class ConfigurableProviderService<C extends ConfigurableProvider, E extends ProviderEntity>
        implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProviderEntityService<E> providerService;

    // keep a local map for system providers since these are not in db
    // key is providerId
    protected final Map<String, C> systemProviders = new HashMap<>();

    protected SmartValidator validator;

    protected Converter<C, E> configConverter;
    protected Converter<E, C> entityConverter;

    public ConfigurableProviderService(ProviderEntityService<E> providerService) {
        Assert.notNull(providerService, "provider entity service is required");

        this.providerService = providerService;
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

    public void setConfigConverter(Converter<C, E> configConverter) {
        this.configConverter = configConverter;
    }

    public void setEntityConverter(Converter<E, C> entityConverter) {
        this.entityConverter = entityConverter;
    }

    protected abstract ConfigurationProvider<?, ?, ?> getConfigurationProvider(String authority)
            throws NoSuchAuthorityException;

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

        List<E> providers = providerService.listProvidersByRealm(realm);
        return providers.stream()
                .map(p -> entityConverter.convert(p))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public C findProvider(String providerId) {
        // lookup in global map first
        if (systemProviders.containsKey(providerId)) {
            return systemProviders.get(providerId);
        }

        E pe = providerService.findProvider(providerId);
        if (pe == null) {
            return null;
        }

        return entityConverter.convert(pe);
    }

    @Transactional(readOnly = true)
    public C getProvider(String providerId)
            throws NoSuchProviderException {
        logger.debug("get provider {}", StringUtils.trimAllWhitespace(providerId));

        // lookup in global map first
        if (systemProviders.containsKey(providerId)) {
            return systemProviders.get(providerId);
        }

        E pe = providerService.getProvider(providerId);
        return entityConverter.convert(pe);
    }

    public C addProvider(String realm,
            C provider)
            throws RegistrationException, SystemException, NoSuchAuthorityException {
        logger.debug("add provider for realm {}", StringUtils.trimAllWhitespace(realm));
        if (logger.isTraceEnabled()) {
            logger.trace("provider bean: " + StringUtils.trimAllWhitespace(provider.toString()));
        }
        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        // check if id provided
        String providerId = provider.getProvider();
        if (StringUtils.hasText(providerId)) {
            E pe = providerService.findProvider(providerId);
            if (pe != null) {
                throw new RegistrationException("id already in use");
            }

            // validate
            if (providerId.length() < 3 || !Pattern.matches(SystemKeys.SLUG_PATTERN, providerId)) {
                throw new RegistrationException("invalid id");
            }

        }

        // unpack props and validate
        E entity = configConverter.convert(provider);

        String authority = provider.getAuthority();

        // we validate config by converting to specific configMap
        ConfigurationProvider<?, ?, ?> configProvider = getConfigurationProvider(authority);
        ConfigMap configurable = configProvider.getConfigMap(provider.getConfiguration());

        // check with validator
        if (validator != null) {
            DataBinder binder = new DataBinder(configurable);
            validator.validate(configurable, binder.getBindingResult());
            if (binder.getBindingResult().hasErrors()) {
                StringBuilder sb = new StringBuilder();
                binder.getBindingResult().getFieldErrors().forEach(e -> {
                    sb.append(e.getField()).append(" ").append(e.getDefaultMessage());
                });
                String errorMsg = sb.toString();
                throw new RegistrationException(errorMsg);
            }
        }

        Map<String, Serializable> configuration = configurable.getConfiguration();

        E pe = providerService.saveProvider(providerId, entity, configuration);
        return entityConverter.convert(pe);
    }

    public C updateProvider(
            String providerId, C provider)
            throws NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("update provider {}", StringUtils.trimAllWhitespace(providerId));
        if (logger.isTraceEnabled()) {
            logger.trace("provider bean: " + StringUtils.trimAllWhitespace(provider.toString()));
        }

        E pe = providerService.getProvider(providerId);

        if (StringUtils.hasText(provider.getProvider()) && !providerId.equals(provider.getProvider())) {
            throw new IllegalArgumentException("configuration does not match provider");
        }

        if (!pe.getAuthority().equals(provider.getAuthority())) {
            throw new IllegalArgumentException("authority mismatch");
        }

        if (!pe.getRealm().equals(provider.getRealm())) {
            throw new IllegalArgumentException("realm mismatch");
        }

        E entity = configConverter.convert(provider);

        String authority = pe.getAuthority();

        // we validate config by converting to specific configMap
        ConfigurationProvider<?, ?, ?> configProvider = getConfigurationProvider(authority);
        ConfigMap configurable = configProvider.getConfigMap(provider.getConfiguration());

        // check with validator
        if (validator != null) {
            DataBinder binder = new DataBinder(configurable);
            validator.validate(configurable, binder.getBindingResult());
            if (binder.getBindingResult().hasErrors()) {
                StringBuilder sb = new StringBuilder();
                binder.getBindingResult().getFieldErrors().forEach(e -> {
                    sb.append(e.getField()).append(" ").append(e.getDefaultMessage());
                });
                String errorMsg = sb.toString();
                throw new RegistrationException(errorMsg);
            }
        }

        Map<String, Serializable> configuration = configurable.getConfiguration();

        E npe = providerService.saveProvider(providerId, entity, configuration);
        return entityConverter.convert(npe);
    }

    public void deleteProvider(String providerId)
            throws SystemException, NoSuchProviderException {
        logger.debug("delete provider {}", StringUtils.trimAllWhitespace(providerId));

        E pe = providerService.getProvider(providerId);
        providerService.deleteProvider(pe.getProvider());
    }

    /*
     * Configuration schemas
     */

    @Transactional(readOnly = true)
    public ConfigurableProperties getConfigurableProperties(String authority) throws NoSuchAuthorityException {
        ConfigurationProvider<?, ?, ?> configProvider = getConfigurationProvider(authority);
        return configProvider.getDefaultConfigMap();
    }

    @Transactional(readOnly = true)
    public JsonSchema getConfigurationSchema(String authority) throws NoSuchAuthorityException {
        ConfigurationProvider<?, ?, ?> configProvider = getConfigurationProvider(authority);
        return configProvider.getSchema();
    }

}
