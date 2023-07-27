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

package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;
import it.smartcommunitylab.aac.core.persistence.ProviderEntityRepository;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

public class ConfigurableProviderEntityService<P extends ProviderEntity> implements ProviderEntityService<P> {

    private final ProviderEntityRepository<P> providerRepository;

    public ConfigurableProviderEntityService(ProviderEntityRepository<P> providerRepository) {
        Assert.notNull(providerRepository, "provider repository is mandatory");
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<P> listProviders() {
        return providerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<P> listProvidersByAuthority(String authority) {
        return providerRepository.findByAuthority(authority);
    }

    @Transactional(readOnly = true)
    public List<P> listProvidersByRealm(String realm) {
        return providerRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public List<P> listProvidersByAuthorityAndRealm(String authority, String realm) {
        return providerRepository.findByAuthorityAndRealm(authority, realm);
    }

    @Transactional(readOnly = true)
    public P findProvider(String providerId) {
        return providerRepository.findByProvider(providerId);
    }

    @Transactional(readOnly = true)
    public P getProvider(String providerId) throws NoSuchProviderException {
        P p = providerRepository.findByProvider(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    public P saveProvider(String providerId, P reg, Map<String, Serializable> configurationMap) {
        P p = reg;

        if (providerId == null) {
            providerId = generateId();
        }

        // always set id
        p.setProvider(providerId);

        // set configMap
        p.setConfigurationMap(configurationMap);

        // save and flush to ensure id is properly persisted
        p = providerRepository.saveAndFlush(p);
        return p;
    }

    public void deleteProvider(String providerId) {
        P p = providerRepository.findByProvider(providerId);
        if (p != null) {
            providerRepository.delete(p);
        }
    }

    /*
     * Helpers
     */
    private String generateId() {
        // generate a random Id with fallback check
        // TODO evaluate UUID replacement
        // generate random
        String id = RandomStringUtils.randomAlphanumeric(8);

        P pe = findProvider(id);
        if (pe != null) {
            // re generate longer
            id = RandomStringUtils.randomAlphanumeric(10);
        }

        return id;
    }
}
