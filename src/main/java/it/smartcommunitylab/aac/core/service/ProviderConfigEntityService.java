/**
 * Copyright 2023 Fondazione Bruno Kessler
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

import it.smartcommunitylab.aac.core.persistence.ProviderConfigEntity;
import it.smartcommunitylab.aac.core.persistence.ProviderConfigEntityRepository;
import it.smartcommunitylab.aac.core.persistence.ProviderConfigId;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/*
 * Service wraps repository to define transaction manager and boundaries
 * we want this isolated from external transactions
 */
@Service
@Transactional(propagation = Propagation.REQUIRED)
public class ProviderConfigEntityService {

    protected final ProviderConfigEntityRepository repository;

    public ProviderConfigEntityService(ProviderConfigEntityRepository repository) {
        Assert.notNull(repository, "config repository is mandatory");
        this.repository = repository;
    }

    public ProviderConfigEntity findOne(@NotNull String type, @NotNull String providerId) {
        ProviderConfigEntity e = repository.findOne(new ProviderConfigId(providerId, type));
        if (e == null) {
            return e;
        }

        return repository.detach(e);
    }

    public Collection<ProviderConfigEntity> findAll(@NotNull String type) {
        return repository.findByType(type).stream().map(repository::detach).collect(Collectors.toList());
    }

    public Collection<ProviderConfigEntity> findByRealm(@NotNull String type, @NotNull String realm) {
        return repository.findByTypeAndRealm(type, realm).stream().map(repository::detach).collect(Collectors.toList());
    }

    public ProviderConfigEntity save(
        @NotNull String type,
        @NotNull String providerId,
        @NotNull ProviderConfigEntity entity
    ) {
        entity.setType(type);
        entity.setProviderId(providerId);

        ProviderConfigEntity e = repository.saveAndFlush(entity);
        return repository.detach(e);
    }

    public void remove(@NotNull String type, @NotNull String providerId) {
        repository.deleteById(new ProviderConfigId(providerId, type));
    }
}
