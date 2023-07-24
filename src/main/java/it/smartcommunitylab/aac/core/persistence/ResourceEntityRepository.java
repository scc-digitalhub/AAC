package it.smartcommunitylab.aac.core.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceEntityRepository extends CustomJpaRepository<ResourceEntity, String> {
    ResourceEntity findByTypeAndId(String type, String id);

    List<ResourceEntity> findByTypeAndAuthority(String type, String authority);

    List<ResourceEntity> findByTypeAndAuthorityAndProvider(String type, String authority, String provider);

    ResourceEntity findByTypeAndAuthorityAndProviderAndResourceId(
        String type,
        String authority,
        String provider,
        String resourceId
    );
}
