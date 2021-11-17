package it.smartcommunitylab.aac.saml.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;



@Repository
public interface SamlUserAccountRepository
        extends CustomJpaRepository<SamlUserAccount, Long>, DetachableJpaRepository<SamlUserAccount> {

    @Query("select u from SamlUserAccount u where u.id=?1")
    SamlUserAccount findByUserId(Long userId);

    SamlUserAccount findByRealmAndProviderAndUserId(String realm, String provider, String userId);

    SamlUserAccount findByRealmAndProviderAndEmail(String realm, String provider, String email);

    List<SamlUserAccount> findBySubject(String subject);

    List<SamlUserAccount> findByRealm(String realm);

    List<SamlUserAccount> findByIssuer(String issuer);

    List<SamlUserAccount> findByProvider(String provider);

    List<SamlUserAccount> findBySubjectAndRealm(String subject, String realm);

    List<SamlUserAccount> findBySubjectAndRealmAndProvider(String subject, String realm, String provider);

}