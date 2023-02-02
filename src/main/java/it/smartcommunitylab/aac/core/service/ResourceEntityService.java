package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.core.persistence.ResourceEntity;
import it.smartcommunitylab.aac.core.persistence.ResourceEntityRepository;

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
            String id, String type,
            String authority, String provider, String resourceId) {
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
    public ResourceEntity findResourceEntity(String type, String authority, String provider,
            String resourceId) {
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

    public void deleteResourceEntity(String type, String authority, String provider,
            String resourceId) {
        ResourceEntity e = resourceRepository.findByTypeAndAuthorityAndProviderAndResourceId(type, authority, provider,
                resourceId);
        if (e != null) {
            resourceRepository.delete(e);
        }
    }

}
