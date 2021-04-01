package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface RealmEntityRepository extends CustomJpaRepository<RealmEntity, String> {

    RealmEntity findBySlug(String slug);

    List<RealmEntity> findBySlugContainingIgnoreCase(String keywords);

    List<RealmEntity> findByNameContainingIgnoreCase(String keywords);

}