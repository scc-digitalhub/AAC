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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.core.persistence.ResourceEntity;
import it.smartcommunitylab.aac.core.persistence.ResourceEntityRepository;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ResourceEntityService {

    private final ResourceEntityRepository resourceRepository;

    // TODO add dynamic resource type registration via beans?
    // private Map<String, ResourceType> types;

    public ResourceEntityService(ResourceEntityRepository resourceRepository) {
        Assert.notNull(resourceRepository, "resource repository is mandatory");
        this.resourceRepository = resourceRepository;
    }

    public String generateId() {
        // generate random
        // TODO ensure unique on multi node deploy: replace with idGenerator
        // (given that UUID is derived from timestamp we consider this safe enough)
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid;
    }

    public ResourceEntity createResourceEntity(String id, String type) {
        if (!StringUtils.hasText(id)) {
            id = generateId();
        }

        // create a subject
        ResourceEntity e = new ResourceEntity(id);
        e.setType(type);

        return e;
    }

    public ResourceEntity addResourceEntity(
        String id,
        String type,
        String authority,
        String provider,
        String resourceId
    ) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("resource id can not be null or empty");
        }

        if (id.length() < 8 || !Pattern.matches(SystemKeys.SLUG_PATTERN, id)) {
            throw new IllegalArgumentException("invalid id");
        }

        // create a subject, will throw error if exists
        ResourceEntity e = new ResourceEntity(id);
        e.setType(type);
        e.setAuthority(authority);
        e.setProvider(provider);
        e.setResourceId(resourceId);

        e = resourceRepository.saveAndFlush(e);
        return e;
    }

    @Transactional(readOnly = true)
    public ResourceEntity getResourceEntity(String id) throws NoSuchResourceException {
        ResourceEntity e = findResourceEntity(id);
        if (e == null) {
            throw new NoSuchResourceException();
        }

        return e;
    }

    @Transactional(readOnly = true)
    public ResourceEntity findResourceEntity(String id) {
        return resourceRepository.findOne(id);
    }

    @Transactional(readOnly = true)
    public ResourceEntity findResourceEntity(String type, String id) {
        return resourceRepository.findByTypeAndId(type, id);
    }

    @Transactional(readOnly = true)
    public ResourceEntity findResourceEntity(String type, String authority, String provider, String resourceId) {
        return resourceRepository.findByTypeAndAuthorityAndProviderAndResourceId(type, authority, provider, resourceId);
    }

    public void deleteResourceEntity(String id) {
        ResourceEntity e = resourceRepository.findOne(id);
        if (e != null) {
            resourceRepository.delete(e);
        }
    }

    public void deleteAllResourceEntities(Collection<String> ids) {
        resourceRepository.deleteAllById(ids);
    }

    public void deleteResourceEntity(String type, String authority, String provider, String resourceId) {
        ResourceEntity e = resourceRepository.findByTypeAndAuthorityAndProviderAndResourceId(
            type,
            authority,
            provider,
            resourceId
        );
        if (e != null) {
            resourceRepository.delete(e);
        }
    }
}
