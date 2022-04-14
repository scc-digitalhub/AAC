package it.smartcommunitylab.aac.openid.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface OIDCUserAccountRepository
        extends CustomJpaRepository<OIDCUserAccount, OIDCUserAccountId>, DetachableJpaRepository<OIDCUserAccount> {

    OIDCUserAccount findByProviderAndUuid(String provider, String uuid);

    List<OIDCUserAccount> findByProviderAndEmail(String provider, String email);

    List<OIDCUserAccount> findByProviderAndUsername(String provider, String username);

    List<OIDCUserAccount> findByRealm(String realm);

    List<OIDCUserAccount> findByProvider(String provider);

    List<OIDCUserAccount> findByUserId(String userId);

    List<OIDCUserAccount> findByUserIdAndRealm(String userId, String realm);

    List<OIDCUserAccount> findByUserIdAndProvider(String userId, String provider);

}