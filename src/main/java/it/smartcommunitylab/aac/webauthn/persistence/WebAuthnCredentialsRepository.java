package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface WebAuthnCredentialsRepository
        extends CustomJpaRepository<WebAuthnCredential, String>, DetachableJpaRepository<WebAuthnCredential> {

    WebAuthnCredential findByCredentialId(String credentialId);

    List<WebAuthnCredential> findByParentAccount(WebAuthnUserAccount parentAccount);

}
