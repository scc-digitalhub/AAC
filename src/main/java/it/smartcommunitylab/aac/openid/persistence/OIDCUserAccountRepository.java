package it.smartcommunitylab.aac.openid.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface OIDCUserAccountRepository
        extends CustomJpaRepository<OIDCUserAccount, Long>, DetachableJpaRepository<OIDCUserAccount> {

    OIDCUserAccount findByRealmAndProviderAndUserId(String realm, String provider, String userId);

    OIDCUserAccount findByRealmAndProviderAndEmail(String realm, String provider, String email);

    List<OIDCUserAccount> findBySubject(String subject);

    List<OIDCUserAccount> findByRealm(String realm);

    List<OIDCUserAccount> findByIssuer(String issuer);

    List<OIDCUserAccount> findByProvider(String provider);

    List<OIDCUserAccount> findBySubjectAndRealm(String subject, String realm);

    List<OIDCUserAccount> findBySubjectAndRealmAndProvider(String subject, String realm, String provider);

}