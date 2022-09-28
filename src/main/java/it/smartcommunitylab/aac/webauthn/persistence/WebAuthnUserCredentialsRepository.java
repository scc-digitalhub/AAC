package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface WebAuthnUserCredentialsRepository
        extends CustomJpaRepository<WebAuthnUserCredential, String>,
        DetachableJpaRepository<WebAuthnUserCredential> {

    WebAuthnUserCredential findByProviderAndUserHandleAndCredentialId(String provider, String userHandle,
            String credentialId);

    List<WebAuthnUserCredential> findByProviderAndCredentialId(String provider, String credentialId);

    List<WebAuthnUserCredential> findByProviderAndUsername(String provider, String username);

    List<WebAuthnUserCredential> findByProviderAndUserHandle(String provider, String userHandle);

}
