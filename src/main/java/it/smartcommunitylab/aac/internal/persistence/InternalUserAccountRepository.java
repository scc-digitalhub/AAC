package it.smartcommunitylab.aac.internal.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface InternalUserAccountRepository
        extends CustomJpaRepository<InternalUserAccount, InternalUserAccountId>,
        DetachableJpaRepository<InternalUserAccount> {

    List<InternalUserAccount> findByProviderAndEmail(String provider, String email);

    InternalUserAccount findByProviderAndConfirmationKey(String provider, String key);

    InternalUserAccount findByProviderAndResetKey(String provider, String key);

    List<InternalUserAccount> findByRealm(String realm);

    List<InternalUserAccount> findByProvider(String provider);

    List<InternalUserAccount> findByUserId(String userId);

    List<InternalUserAccount> findByUserIdAndRealm(String userId, String realm);

    List<InternalUserAccount> findByUserIdAndProvider(String userId, String provider);

}
