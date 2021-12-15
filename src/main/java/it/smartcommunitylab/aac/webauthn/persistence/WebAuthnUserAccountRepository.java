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

        WebAuthnUserAccount findByCredentialId(ByteArray credentialId);

        List<WebAuthnUserAccount> findByRealm(String realm);

        WebAuthnUserAccount findByUserHandle(ByteArray userHandle);

        WebAuthnUserAccount findByUserId(String userId);

        WebAuthnUserAccount findByRealmAndEmail(String realm, String email);

}
