package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface UserAttributeEntityRepository extends CustomJpaRepository<UserAttributeEntity, Long> {

    List<UserAttributeEntity> findByAuthority(String authority);

    List<UserAttributeEntity> findByAuthorityAndUserId(String authority, String userId);

    List<UserAttributeEntity> findByAuthorityAndProvider(String authority, String provider);

    List<UserAttributeEntity> findByAuthorityAndProviderAndUserId(String authority, String provider, String userId);

    UserAttributeEntity findByAuthorityAndProviderAndUserIdAndKey(String authority, String provider, String userId,
            String key);

}
