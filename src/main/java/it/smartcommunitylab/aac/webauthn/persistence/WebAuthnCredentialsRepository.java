package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface WebAuthnCredentialsRepository
        extends CustomJpaRepository<WebAuthnCredential, String>,
        DetachableJpaRepository<WebAuthnCredential> {

    WebAuthnCredential findByProviderAndUserHandleAndCredentialId(String provider, String userHandle,
            String credentialId);

    List<WebAuthnCredential> findByProviderAndCredentialId(String provider, String credentialId);

    List<WebAuthnCredential> findByProviderAndUsername(String provider, String username);

    List<WebAuthnCredential> findByProviderAndUserHandle(String provider, String userHandle);

}
