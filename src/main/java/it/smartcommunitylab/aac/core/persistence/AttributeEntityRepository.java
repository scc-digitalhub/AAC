package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface AttributeEntityRepository extends CustomJpaRepository<AttributeEntity, Long> {

    AttributeEntity findBySetAndKey(String id, String key);

    List<AttributeEntity> findBySet(String id);

}
