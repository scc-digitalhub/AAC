package it.smartcommunitylab.aac.roles.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface RealmRoleEntityRepository extends CustomJpaRepository<RealmRoleEntity, String> {
    RealmRoleEntity findByRealmAndRole(String realm, String role);

    List<RealmRoleEntity> findByRealm(String realm);
}
