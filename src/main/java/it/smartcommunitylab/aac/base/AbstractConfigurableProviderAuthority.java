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

package it.smartcommunitylab.aac.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.config.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.config.AbstractConfigurableProviderI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public abstract class AbstractConfigurableProviderAuthority<
    S extends ConfigurableResourceProvider<R, T, M, C>,
    R extends Resource,
    T extends AbstractConfigurableProviderI,
    M extends AbstractConfigMap,
    C extends AbstractProviderConfig<M, T>
>
    extends AbstractProviderAuthority<S, R, T, M, C>
    implements ConfigurableProviderAuthority<S, R, T, M, C>, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AbstractConfigurableProviderAuthority(
        String authorityId,
        ProviderConfigRepository<C> registrationRepository
    ) {
        super(authorityId, registrationRepository);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(getConfigurationProvider(), "config provider is mandatory");
    }

    @Override
    public abstract ConfigurationProvider<M, T, C> getConfigurationProvider();

    @Override
    public C registerProvider(AbstractConfigurableProvider cp) throws RegistrationException {
        // cast config and handle errors
        T tcp = null;
        try {
            @SuppressWarnings("unchecked")
            T t = (T) cp;
            tcp = t;
        } catch (ClassCastException e) {
            logger.error("Wrong config class: " + e.getMessage());
            throw new IllegalArgumentException("unsupported configurable class");
        }

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
                C e = registrationRepository.findByProviderId(providerId);
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
                        S rp = providers.get(providerId);

                        // return effective config
                        return rp.getConfig();
                    } else if (e.getVersion() > cp.getVersion()) {
                        throw new RegistrationException("invalid version");
                    }
                }

                // build config
                C providerConfig = getConfigurationProvider().getConfig(tcp);
                if (logger.isTraceEnabled()) {
                    logger.trace(
                        "provider active config v{}: {}",
                        providerConfig.getVersion(),
                        String.valueOf(providerConfig.getConfigMap().getConfiguration())
                    );
                }

                //TODO add validation

                // register, we defer loading
                // should update if existing
                registrationRepository.addRegistration(providerConfig);

                // load to warm cache
                S rp = providers.get(providerId);

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
        C registration = registrationRepository.findByProviderId(providerId);

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
}
