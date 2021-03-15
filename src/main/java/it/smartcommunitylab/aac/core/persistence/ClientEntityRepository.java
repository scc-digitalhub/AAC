package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface ClientEntityRepository extends CustomJpaRepository<ClientEntity, Long> {

    ClientEntity findByClientId(String clientId);

    List<ClientEntity> findByRealm(String realm);

}
