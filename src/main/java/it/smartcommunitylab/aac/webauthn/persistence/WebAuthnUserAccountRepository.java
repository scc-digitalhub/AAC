package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface WebAuthnUserAccountRepository
                extends CustomJpaRepository<WebAuthnUserAccount, Long>, DetachableJpaRepository<WebAuthnUserAccount> {

        WebAuthnUserAccount findByRealmAndUsername(String realm, String username);

        WebAuthnUserAccount findByProviderAndUsername(String provider, String username);

        WebAuthnUserAccount findBySubjectAndRealm(String subject, String realm);

        List<WebAuthnUserAccount> findByRealm(String realm);

        WebAuthnUserAccount findByUserHandle(String userHandle);

        WebAuthnUserAccount findBySubject(String userId);

        WebAuthnUserAccount findByRealmAndEmailAddress(String realm, String email);
}
