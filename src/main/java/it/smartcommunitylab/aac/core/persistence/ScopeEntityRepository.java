package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface ScopeEntityRepository
        extends CustomJpaRepository<ScopeEntity, Long> {

    ScopeEntity findByScope(String scope);

    List<ScopeEntity> findByResourceId(String resourceId);

    List<ScopeEntity> findByResourceIdAndType(String resourceId, String type);
}
