package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface AttributeEntityRepository extends CustomJpaRepository<AttributeEntity, Long> {

    List<AttributeEntity> findByAuthority(String authority);

    List<AttributeEntity> findByAuthorityAndUserId(String authority, String userId);

    List<AttributeEntity> findByAuthorityAndProvider(String authority, String provider);

    List<AttributeEntity> findByAuthorityAndProviderAndUserId(String authority, String provider, String userId);

    AttributeEntity findByAuthorityAndProviderAndUserIdAndKey(String authority, String provider, String userId,
            String key);

}
