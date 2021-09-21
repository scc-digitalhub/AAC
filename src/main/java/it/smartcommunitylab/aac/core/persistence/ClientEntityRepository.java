package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface ClientEntityRepository extends CustomJpaRepository<ClientEntity, Long> {

    ClientEntity findByClientId(String clientId);

    List<ClientEntity> findByRealmAndName(String realm, String name);

    List<ClientEntity> findByRealm(String realm);

    List<ClientEntity> findByRealmAndType(String realm, String type);

    Page<ClientEntity> findByRealm(String realm, Pageable page);

    Page<ClientEntity> findByRealmAndNameContainingIgnoreCase(String realm, String name, Pageable page);

    Page<ClientEntity> findByRealmAndNameContainingIgnoreCaseOrRealmAndClientIdContainingIgnoreCase(String realmn,
            String name,
            String realmc, String clientId, Pageable page);

    long countByRealm(String realm);

}
