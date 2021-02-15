package it.smartcommunitylab.aac.internal.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface InternalUserAccountRepository extends CustomJpaRepository<InternalUserAccount, Long> {

    InternalUserAccount findByRealmAndUserId(String realm, String userId);

    InternalUserAccount findByConfirmationKey(String key);

    InternalUserAccount findByResetKey(String key);

    List<InternalUserAccount> findBySubject(String subject);

    List<InternalUserAccount> findByRealm(String realm);

    List<InternalUserAccount> findByUserId(String userId);
}
