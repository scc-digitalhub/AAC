package it.smartcommunitylab.aac.attributes.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface AttributeSetEntityRepository extends CustomJpaRepository<AttributeSetEntity, Long> {
    AttributeSetEntity findByIdentifier(String identifier);

    List<AttributeSetEntity> findByRealm(String realm);

    List<AttributeSetEntity> findByNameContainingIgnoreCase(String keywords);
}
