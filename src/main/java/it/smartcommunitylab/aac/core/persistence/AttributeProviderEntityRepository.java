package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface AttributeProviderEntityRepository extends CustomJpaRepository<AttributeProviderEntity, String> {

    AttributeProviderEntity findByProviderId(String providerId);

    List<AttributeProviderEntity> findByAuthority(String authority);

    List<AttributeProviderEntity> findByRealm(String realm);

    List<AttributeProviderEntity> findByAuthorityAndRealm(String authority, String realm);

}
