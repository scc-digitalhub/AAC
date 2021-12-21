package it.smartcommunitylab.aac.webauthn.persistence;

import com.yubico.webauthn.data.ByteArray;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface WebAuthnCredentialsRepository
        extends CustomJpaRepository<WebAuthnCredential, ByteArray>, DetachableJpaRepository<WebAuthnCredential> {

    WebAuthnCredential findByCredentialId(ByteArray credentialId);

}
