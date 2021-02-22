package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface AttributeEntityRepository extends CustomJpaRepository<AttributeEntity, Long> {

    List<AttributeEntity> findBySubject(String subject);

    List<AttributeEntity> findByAuthority(String authority);

    List<AttributeEntity> findBySubjectAndAuthority(String subject, String authority);

    List<AttributeEntity> findByAuthorityAndUserId(String authority, Long userId);

    List<AttributeEntity> findBySubjectAndAuthorityAndUserId(String subject, String authority, Long userId);

    List<AttributeEntity> findBySubjectAndAuthorityAndProviderAndUserId(String subject, String authority,
            String provider, Long userId);

    AttributeEntity findByAuthorityAndProviderAndUserIdAndKey(String authority, String provider, Long userId,
            String key);

}
