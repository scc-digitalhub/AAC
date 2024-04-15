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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.model.AbstractSettingsMap;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurableResourceProvider;
import it.smartcommunitylab.aac.base.provider.config.AbstractProviderConfig;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.authorities.ProviderAuthority;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public abstract class AbstractProviderAuthority<
    P extends AbstractConfigurableResourceProvider<
        ? extends Resource,
        C,
        ? extends AbstractSettingsMap,
        ? extends AbstractConfigMap
    >,
    C extends AbstractProviderConfig<? extends AbstractSettingsMap, ? extends AbstractConfigMap>
>
    implements ProviderAuthority<P> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String authorityId;

    // provider configs by id
    protected final ProviderConfigRepository<C> registrationRepository;

    // loading cache for idps
    // TODO replace with external loadableProviderRepository for
    // ProviderRepository<InternalIdentityProvider>
    protected final LoadingCache<String, P> providers = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
        .maximumSize(100)
        .build(
            new CacheLoader<String, P>() {
                @Override
                public P load(final String id) throws Exception {
                    logger.debug("load config from repository for {}", id);
                    try {
                        C config = registrationRepository.findByProviderId(id);

                        if (config == null) {
                            throw new IllegalArgumentException("no configuration matching the given provider id");
                        }

                        logger.debug("build provider {} config", id);
                        return buildProvider(config);
                    } catch (ClassCastException ce) {
                        throw new IllegalArgumentException("no configuration matching the given provider id");
                    }
                }
            }
        );

    protected AbstractProviderAuthority(String authorityId, ProviderConfigRepository<C> registrationRepository) {
        Assert.hasText(authorityId, "authority id  is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository is mandatory");

        this.authorityId = authorityId;
        this.registrationRepository = registrationRepository;
    }

    @Override
    public String getAuthorityId() {
        return authorityId;
    }

    @Override
    public String getId() {
        return authorityId;
    }

    protected abstract P buildProvider(C config);

    @Override
    public boolean hasProvider(String providerId) {
        C registration = registrationRepository.findByProviderId(providerId);
        return (registration != null);
    }

    @Override
    public P findProvider(String providerId) {
        Assert.hasText(providerId, "provider id can not be null or empty");

        try {
            // check if config is still active
            C config = registrationRepository.findByProviderId(providerId);
            if (config == null) {
                // cleanup cache
                providers.invalidate(providerId);

                return null;
            }

            P p = providers.get(providerId);

            // check config version match against repo
            if (config.getVersion() > p.getConfig().getVersion()) {
                // invalidate cache and rebuild
                providers.invalidate(providerId);

                p = providers.get(providerId);
            }

            return p;
        } catch (IllegalArgumentException | UncheckedExecutionException | ExecutionException e) {
            return null;
        }
    }

    @Override
    public P getProvider(String providerId) throws NoSuchProviderException {
        Assert.hasText(providerId, "provider id can not be null or empty");
        P p = findProvider(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    @Override
    public List<P> getProvidersByRealm(String realm) {
        // we need to fetch registrations and get idp from cache, with optional load
        Collection<C> registrations = registrationRepository.findByRealm(realm);
        return registrations
            .stream()
            .map(r -> findProvider(r.getProvider()))
            .filter(p -> (p != null))
            .collect(Collectors.toList());
    }
}
