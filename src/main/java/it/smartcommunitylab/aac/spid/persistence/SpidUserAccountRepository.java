package it.smartcommunitylab.aac.spid.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface SpidUserAccountRepository
        extends CustomJpaRepository<SpidUserAccount, Long>, DetachableJpaRepository<SpidUserAccount> {

    @Query("select u from SpidUserAccount u where u.id=?1")
    SpidUserAccount findByUserId(Long userId);

    SpidUserAccount findByRealmAndProviderAndUserId(String realm, String provider, String userId);

    List<SpidUserAccount> findBySubject(String subject);

    List<SpidUserAccount> findByRealm(String realm);

    List<SpidUserAccount> findByIssuer(String issuer);

    List<SpidUserAccount> findByProvider(String provider);

    List<SpidUserAccount> findBySubjectAndRealm(String subject, String realm);

    List<SpidUserAccount> findBySubjectAndRealmAndProvider(String subject, String realm, String provider);

}