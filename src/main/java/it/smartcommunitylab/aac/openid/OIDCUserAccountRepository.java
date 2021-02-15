package it.smartcommunitylab.aac.openid;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface OIDCUserAccountRepository extends CustomJpaRepository<OIDCUserAccount, Long> {

    OIDCUserAccount findByRealmAndUserId(String realm, String userId);

    OIDCUserAccount findByConfirmationKey(String key);

    List<OIDCUserAccount> findBySubject(String subject);

    List<OIDCUserAccount> findByRealm(String realm);

    List<OIDCUserAccount> findByProviderId(String providerId);

    List<OIDCUserAccount> findByIssuer(String issuer);

}
