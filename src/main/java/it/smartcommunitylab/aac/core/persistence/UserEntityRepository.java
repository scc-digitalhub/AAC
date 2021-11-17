package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface UserEntityRepository extends CustomJpaRepository<UserEntity, Long> {

    UserEntity findByUuid(String uuid);

    List<UserEntity> findByRealm(String realm);

    List<UserEntity> findByRealmAndUsername(String realm, String username);

    List<UserEntity> findByRealmAndEmailAddress(String realm, String emailAddress);

//    Page<UserEntity> findByRealm(String realm, Pageable pageRequest);
//    @Query("select u from UserEntity u where u.realm = ?1 and LOWER(u.username) like lower(concat('%', ?2,'%'))")
//    Page<UserEntity> findByRealm(String realm, String q, Pageable pageRequest);

//    @Query("select distinct u from UserEntity u left outer join UserRoleEntity r on u.uuid = r.subject where (r.realm = ?1 or u.realm = ?1)")
    Page<UserEntity> findByRealm(String realm, Pageable pageRequest);

//    @Query("select distinct u from UserEntity u  left outer join UserRoleEntity r on u.uuid = r.subject where (r.realm = ?1 or u.realm = ?1) and LOWER(u.username) like lower(concat('%', ?2,'%'))")
    Page<UserEntity> findByRealmAndUsernameContainingIgnoreCase(String realm, String q, Pageable pageRequest);

    Page<UserEntity> findByRealmAndUsernameContainingIgnoreCaseOrRealmAndUuidContainingIgnoreCaseOrRealmAndEmailAddressContainingIgnoreCase(
            String realmn, String name,
            String realmu, String uuid,
            String realme, String email,
            Pageable page);

    long countByRealm(String realm);

}
