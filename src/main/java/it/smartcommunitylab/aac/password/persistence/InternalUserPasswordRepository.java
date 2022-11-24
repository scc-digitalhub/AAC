package it.smartcommunitylab.aac.password.persistence;

import java.util.List;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

public interface InternalUserPasswordRepository extends CustomJpaRepository<InternalUserPassword, String>,
        DetachableJpaRepository<InternalUserPassword> {

    List<InternalUserPassword> findByRealm(String realm);

    List<InternalUserPassword> findByRepositoryIdAndUserId(String repositoryId, String userId);

    InternalUserPassword findByRepositoryIdAndUsernameAndStatusOrderByCreateDateDesc(String repositoryId,
            String username,
            String status);

    InternalUserPassword findByRepositoryIdAndResetKey(String repositoryId, String key);

    List<InternalUserPassword> findByRepositoryIdAndUsernameOrderByCreateDateDesc(String repositoryId, String username);

}
