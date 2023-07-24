package it.smartcommunitylab.aac.attributes.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface AttributeEntityRepository extends CustomJpaRepository<AttributeEntity, Long> {
    AttributeEntity findBySetAndKey(String id, String key);

    List<AttributeEntity> findBySetOrderById(String id);
}
