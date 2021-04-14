package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface RealmRoleEntityRepository extends CustomJpaRepository<RealmRoleEntity, String> {

    RealmRoleEntity findByRealmAndRole(String realm, String role);

    List<RealmRoleEntity> findByRealm(String realm);

}