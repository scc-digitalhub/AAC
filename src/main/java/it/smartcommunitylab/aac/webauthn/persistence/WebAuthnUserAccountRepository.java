package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface WebAuthnUserAccountRepository
                extends CustomJpaRepository<WebAuthnUserAccount, String>, DetachableJpaRepository<WebAuthnUserAccount> {

        // WebAuthnUserAccount findByRealmAndUsername(String realm, String username);
        // use findByProviderAndUsername

        WebAuthnUserAccount findByProviderAndUsername(String provider, String username);

        // A subject can have more accounts
        List<WebAuthnUserAccount> findBySubjectAndRealm(String subject, String realm);

        WebAuthnUserAccount findByUserHandle(String userHandle);

        List<WebAuthnUserAccount> findBySubject(String subject);
}
