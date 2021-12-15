package it.smartcommunitylab.aac.webauthn.persistence;

import java.util.List;

import com.yubico.webauthn.data.ByteArray;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface WebAuthnUserAccountRepository
                extends CustomJpaRepository<WebAuthnUserAccount, Long>, DetachableJpaRepository<WebAuthnUserAccount> {

        WebAuthnUserAccount findByRealmAndUsername(String realm, String username);

        List<WebAuthnUserAccount> findBySubjectAndRealm(String subject, String realm);

        WebAuthnUserAccount findByCredentialCredentialId(ByteArray credentialId);

        List<WebAuthnUserAccount> findByRealm(String realm);

        WebAuthnUserAccount findByCredentialUserHandle(ByteArray userHandle);

        WebAuthnUserAccount findBySubject(String userId);

        WebAuthnUserAccount findByRealmAndEmailAddress(String realm, String email);

}
