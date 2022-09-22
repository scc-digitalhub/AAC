package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface IdentityServiceEntityRepository extends CustomJpaRepository<IdentityServiceEntity, String> {

    IdentityServiceEntity findByProviderId(String providerId);

    List<IdentityServiceEntity> findByAuthority(String authority);

    List<IdentityServiceEntity> findByRealm(String realm);

    List<IdentityServiceEntity> findByAuthorityAndRealm(String authority, String realm);

}
