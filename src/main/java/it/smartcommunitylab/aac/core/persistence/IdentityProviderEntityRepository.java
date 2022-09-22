package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface IdentityProviderEntityRepository extends CustomJpaRepository<IdentityProviderEntity, String> {

    IdentityProviderEntity findByProviderId(String providerId);

    List<IdentityProviderEntity> findByAuthority(String authority);

    List<IdentityProviderEntity> findByRealm(String realm);

    List<IdentityProviderEntity> findByAuthorityAndRealm(String authority, String realm);

}
