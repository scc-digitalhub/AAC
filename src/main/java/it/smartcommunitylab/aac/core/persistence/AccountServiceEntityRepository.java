package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface AccountServiceEntityRepository extends CustomJpaRepository<AccountServiceEntity, String> {

    AccountServiceEntity findByProviderId(String providerId);

    List<AccountServiceEntity> findByAuthority(String authority);

    List<AccountServiceEntity> findByRealm(String realm);

    List<AccountServiceEntity> findByAuthorityAndRealm(String authority, String realm);

}
