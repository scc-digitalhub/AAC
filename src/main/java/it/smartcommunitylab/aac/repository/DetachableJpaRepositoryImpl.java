package it.smartcommunitylab.aac.repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class DetachableJpaRepositoryImpl<T> implements DetachableJpaRepository<T> {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public <S extends T> S detach(S e) {
        entityManager.detach(e);
        return e;
    }
}
