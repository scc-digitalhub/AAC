package it.smartcommunitylab.aac.internal.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface InternalUserAccountRepository extends CustomJpaRepository<InternalUserAccount, Long> {

    @Query("select u from InternalUserAccount u where u.id=?1")
    InternalUserAccount findByUserId(Long userId);

    InternalUserAccount findByRealmAndUsername(String realm, String username);

    InternalUserAccount findByConfirmationKey(String key);

    InternalUserAccount findByResetKey(String key);

    List<InternalUserAccount> findBySubject(String subject);

    List<InternalUserAccount> findByRealm(String realm);

    List<InternalUserAccount> findByRealmAndEmail(String realm, String email);

}
