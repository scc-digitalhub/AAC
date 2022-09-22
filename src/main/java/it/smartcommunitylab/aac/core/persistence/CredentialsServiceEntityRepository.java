package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface CredentialsServiceEntityRepository extends CustomJpaRepository<CredentialsServiceEntity, String> {

    CredentialsServiceEntity findByProviderId(String providerId);

    List<CredentialsServiceEntity> findByAuthority(String authority);

    List<CredentialsServiceEntity> findByRealm(String realm);

    List<CredentialsServiceEntity> findByAuthorityAndRealm(String authority, String realm);

}
