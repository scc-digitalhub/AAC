package it.smartcommunitylab.aac.internal.persistence;

import java.util.List;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

public interface InternalUserPasswordRepository extends CustomJpaRepository<InternalUserPassword, String>,
        DetachableJpaRepository<InternalUserPassword> {

    InternalUserPassword findByProviderAndUsernameAndStatusOrderByCreateDateDesc(String provider, String username,
            String status);

    InternalUserPassword findByProviderAndResetKey(String provider, String key);

    InternalUserPassword findByProviderAndUsernameAndPassword(String provider, String username, String password);

    List<InternalUserPassword> findByProviderAndUsernameOrderByCreateDateDesc(String provider, String username);

}
