package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface UserEntityRepository extends CustomJpaRepository<UserEntity, Long> {

    UserEntity findByUuid(String uuid);

    List<UserEntity> findByRealm(String realm);

    Page<UserEntity> findByRealm(String realm, Pageable pageRequest);

    @Query("select u from UserEntity u where u.realm = ?1 and LOWER(u.username) like lower(concat('%', ?2,'%'))")
    Page<UserEntity> findByRealm(String realm, String q, Pageable pageRequest);

    long countByRealm(String realm);

}
