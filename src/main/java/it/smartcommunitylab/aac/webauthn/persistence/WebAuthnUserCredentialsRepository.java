package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface WebAuthnUserCredentialsRepository extends CustomJpaRepository<WebAuthnUserCredential, String>,
        DetachableJpaRepository<WebAuthnUserCredential> {

    List<WebAuthnUserCredential> findByRealm(String realm);

    List<WebAuthnUserCredential> findByRepositoryIdAndUserId(String repositoryId, String userId);

    WebAuthnUserCredential findByRepositoryIdAndUserHandleAndCredentialId(String repositoryId, String userHandle,
            String credentialId);

    List<WebAuthnUserCredential> findByRepositoryIdAndCredentialId(String repositoryId, String credentialId);

    List<WebAuthnUserCredential> findByRepositoryIdAndUsername(String repositoryId, String username);

    List<WebAuthnUserCredential> findByRepositoryIdAndUserHandle(String repositoryId, String userHandle);

}
