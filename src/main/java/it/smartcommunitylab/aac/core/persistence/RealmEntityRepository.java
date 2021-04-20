package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface RealmEntityRepository extends CustomJpaRepository<RealmEntity, String> {

    RealmEntity findBySlug(String slug);

    List<RealmEntity> findBySlugContainingIgnoreCase(String keywords);
    List<RealmEntity> findByNameContainingIgnoreCase(String keywords);

    @Query("select r from RealmEntity r where lower(r.slug) like lower(concat('%', ?1,'%')) or LOWER(r.name) like lower(concat('%', ?1,'%'))")
    Page<RealmEntity> findByKeywords(String keywords, Pageable pageRequest);
}