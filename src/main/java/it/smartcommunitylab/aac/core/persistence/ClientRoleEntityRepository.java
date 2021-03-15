package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface ClientRoleEntityRepository extends CustomJpaRepository<ClientRoleEntity, Long> {

    List<ClientRoleEntity> findByClientId(String clientId);

    List<ClientRoleEntity> findByClientIdAndRealm(String clientId, String realm);

}
