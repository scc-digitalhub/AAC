package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface WebAuthnCredentialsRepository
        extends CustomJpaRepository<WebAuthnCredential, WebAuthnCredentialId>,
        DetachableJpaRepository<WebAuthnCredential> {

    List<WebAuthnCredential> findByProviderAndUserHandle(String provider, String userHandle);

    WebAuthnCredential findByProviderAndUserHandleAndCredentialId(String provider, String userHandle,
            String credentialId);
}
