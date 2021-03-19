package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface ProviderEntityRepository extends CustomJpaRepository<ProviderEntity, Long> {

    ProviderEntity findByProviderId(String providerId);

    List<ProviderEntity> findByAuthority(String authority);

    List<ProviderEntity> findByRealm(String realm);

    List<ProviderEntity> findByAuthorityAndType(String authority, String type);

    List<ProviderEntity> findByRealmAndType(String realm, String type);

    List<ProviderEntity> findByAuthorityAndRealm(String authority, String realm);

    List<ProviderEntity> findByAuthorityAndRealmAndType(String authority, String realm, String type);

}
