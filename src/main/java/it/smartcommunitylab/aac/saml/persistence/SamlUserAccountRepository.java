package it.smartcommunitylab.aac.saml.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface SamlUserAccountRepository extends CustomJpaRepository<SamlUserAccount, SamlUserAccountId>,
        DetachableJpaRepository<SamlUserAccount> {

    SamlUserAccount findByProviderAndUuid(String provider, String uuid);

    List<SamlUserAccount> findByProviderAndEmail(String provider, String email);

    List<SamlUserAccount> findByProviderAndUsername(String provider, String username);

    List<SamlUserAccount> findByRealm(String realm);

    List<SamlUserAccount> findByProvider(String provider);

    List<SamlUserAccount> findByUserId(String userId);

    List<SamlUserAccount> findByUserIdAndRealm(String userId, String realm);

    List<SamlUserAccount> findByUserIdAndProvider(String userId, String provider);

}