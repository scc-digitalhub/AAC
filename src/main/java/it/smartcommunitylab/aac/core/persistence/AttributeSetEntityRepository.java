package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface AttributeSetEntityRepository extends CustomJpaRepository<AttributeSetEntity, Long> {
    AttributeSetEntity findBySet(String identifier);

    List<AttributeSetEntity> findByNameContainingIgnoreCase(String keywords);
}
