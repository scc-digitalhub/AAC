package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface WebAuthnUserAccountRepository
        extends CustomJpaRepository<WebAuthnUserAccount, WebAuthnUserAccountId>,
        DetachableJpaRepository<WebAuthnUserAccount> {

    WebAuthnUserAccount findByProviderAndUuid(String provider, String uuid);

    WebAuthnUserAccount findByProviderAndConfirmationKey(String provider, String key);

    WebAuthnUserAccount findByProviderAndUserHandle(String provider, String userHandle);

    List<WebAuthnUserAccount> findByProviderAndEmailAddress(String subject, String emailAddress);

    List<WebAuthnUserAccount> findByRealm(String realm);

    List<WebAuthnUserAccount> findByProvider(String provider);

    List<WebAuthnUserAccount> findByUserId(String userId);

    List<WebAuthnUserAccount> findByUserIdAndRealm(String userId, String realm);

    List<WebAuthnUserAccount> findByUserIdAndProvider(String userId, String provider);
}
