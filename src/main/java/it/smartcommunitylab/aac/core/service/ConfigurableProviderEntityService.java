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
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ConfigurableProviderEntityService {

    protected final ProviderEntityRepository providerRepository;

    public ConfigurableProviderEntityService(ProviderEntityRepository providerRepository) {
        Assert.notNull(providerRepository, "provider repository is mandatory");

        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<ProviderEntity> listProviders(String type) {
        return providerRepository.findByType(type);
    }

    @Transactional(readOnly = true)
    public List<ProviderEntity> listProvidersByRealm(String realm) {
        return providerRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public List<ProviderEntity> listProvidersByRealm(String type, String realm) {
        return providerRepository.findByTypeAndRealm(type, realm);
    }

    @Transactional(readOnly = true)
    public List<ProviderEntity> listProvidersByAuthority(String type, String authority) {
        return providerRepository.findByTypeAndAuthority(type, authority);
    }

    @Transactional(readOnly = true)
    public List<ProviderEntity> listProvidersByAuthorityAndRealm(String type, String authority, String realm) {
        return providerRepository.findByTypeAndAuthorityAndRealm(type, authority, realm);
    }

    @Transactional(readOnly = true)
    public ProviderEntity findProvider(String type, String providerId) {
        ProviderEntity p = providerRepository.findByProvider(providerId);
        if (p == null || !type.equals(p.getType())) {
            return null;
        }

        return p;
    }

    @Transactional(readOnly = true)
    public ProviderEntity getProvider(String type, String providerId) throws NoSuchProviderException {
        ProviderEntity p = findProvider(type, providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    //TODO drop nullable id, we shoulds always receive it
    public ProviderEntity saveProvider(String type, @Nullable String providerId, ProviderEntity reg) {
        if (reg == null) {
            throw new IllegalArgumentException();
        }

        if (StringUtils.hasText(reg.getType()) && !type.equals(reg.getType())) {
            throw new IllegalArgumentException();
        }

        if (providerId == null) {
            providerId = generateId();
        }

        ProviderEntity p = providerRepository.findByProvider(providerId);
        if (p == null) {
            p = new ProviderEntity();
            p.setProvider(providerId);
            p.setType(type);
        } else {
            if (!type.equals(p.getType())) {
                throw new IllegalArgumentException();
            }
        }

        //set base
        p.setAuthority(reg.getAuthority());
        p.setRealm(p.getRealm());

        p.setName(reg.getName());
        p.setTitleMap(reg.getTitleMap());
        p.setDescriptionMap(reg.getDescriptionMap());

        //set config
        int version = reg.getVersion() != null ? reg.getVersion().intValue() : 0;
        p.setSettingsMap(reg.getSettingsMap());
        p.setConfigurationMap(reg.getConfigurationMap());
        p.setVersion(version);

        //status
        boolean enabled = reg.getEnabled() != null ? reg.getEnabled().booleanValue() : false;
        p.setEnabled(enabled);

        // save and flush to ensure id is properly persisted
        p = providerRepository.saveAndFlush(p);
        return p;
    }

    public void deleteProvider(String type, String providerId) {
        ProviderEntity p = providerRepository.findByProvider(providerId);
        if (p != null && type.equals(p.getType())) {
            providerRepository.delete(p);
        }
    }

    /*
     * Helpers
     * TODO drop from here
     */
    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
