package it.smartcommunitylab.aac.internal.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface InternalUserAccountRepository
        extends CustomJpaRepository<InternalUserAccount, Long>, DetachableJpaRepository<InternalUserAccount> {

//    @Query("select u from InternalUserAccount u where u.id=?1")
//    InternalUserAccount findByUserId(Long userId);

    InternalUserAccount findByRealmAndUsername(String realm, String username);

    InternalUserAccount findByRealmAndConfirmationKey(String realm, String key);

    InternalUserAccount findByRealmAndResetKey(String realm, String key);

    List<InternalUserAccount> findBySubject(String subject);

    List<InternalUserAccount> findBySubjectAndRealm(String subject, String realm);

    List<InternalUserAccount> findByRealm(String realm);

    InternalUserAccount findByRealmAndEmail(String realm, String email);

}
