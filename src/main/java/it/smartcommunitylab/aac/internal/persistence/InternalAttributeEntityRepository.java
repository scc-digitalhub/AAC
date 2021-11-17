package it.smartcommunitylab.aac.internal.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface InternalAttributeEntityRepository extends CustomJpaRepository<InternalAttributeEntity, Long> {

    List<InternalAttributeEntity> findByProvider(String provider);

    List<InternalAttributeEntity> findByProviderAndSubjectId(String provider, String userId);

    List<InternalAttributeEntity> findByProviderAndSubjectIdAndSet(String provider, String userId, String setId);

    InternalAttributeEntity findByProviderAndSubjectIdAndSetAndKey(String provider, String userId, String setId,
            String key);

}
