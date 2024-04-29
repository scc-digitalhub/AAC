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

package it.smartcommunitylab.aac.base.authorities;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.model.AbstractSettingsMap;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurableResourceProvider;
import it.smartcommunitylab.aac.base.provider.config.AbstractProviderConfig;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.model.ConfigMap;
import it.smartcommunitylab.aac.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;
import org.springframework.validation.SmartValidator;

public abstract class AbstractConfigurableProviderAuthority<
    RP extends AbstractConfigurableResourceProvider<? extends Resource, P, S, M>,
    C extends ConfigurableProvider<S>,
    P extends AbstractProviderConfig<S, M>,
    S extends AbstractSettingsMap,
    M extends AbstractConfigMap
>
    extends AbstractProviderAuthority<RP, P>
    implements ConfigurableProviderAuthority<RP, C, P, S, M>, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected SmartValidator validator;

    protected AbstractConfigurableProviderAuthority(
        String authorityId,
        ProviderConfigRepository<P> registrationRepository
    ) {
        super(authorityId, registrationRepository);
    }

    @Autowired
    public void setValidator(SmartValidator validator) {
        this.validator = validator;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Assert.notNull(getConfigurationProvider(), "config provider is mandatory");
    }

    @Override
    public P registerProvider(C cp) throws IllegalArgumentException, RegistrationException, SystemException {
        // we support only matching provider as resource providers
        if (cp != null && getAuthorityId().equals(cp.getAuthority())) {
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
                        // same version, already registered, nothing to do
                        // load to warm cache
                        RP rp = providers.get(providerId);

                        // return effective config
                        return rp.getConfig();
                    } else if (e.getVersion() > cp.getVersion()) {
                        throw new RegistrationException("invalid version");
                    }
                }

                // build config
                P providerConfig = getConfigurationProvider().getConfig(cp);
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

                // register, we defer loading
                // should update if existing
                registrationRepository.addRegistration(providerConfig);

                // load to warm cache
                RP rp = providers.get(providerId);

                // return effective config
                return rp.getConfig();
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
    public void unregisterProvider(String providerId) {
        P registration = registrationRepository.findByProviderId(providerId);

        if (registration != null) {
            // can't unregister system providers, check
            if (SystemKeys.REALM_SYSTEM.equals(registration.getRealm())) {
                return;
            }

            logger.debug("unregister provider {} for realm {}", providerId, registration.getRealm());

            // remove from cache
            providers.invalidate(providerId);

            // remove from registrations
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
}
