package it.smartcommunitylab.aac.internal.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface InternalUserAccountRepository
    extends
        CustomJpaRepository<InternalUserAccount, InternalUserAccountId>, DetachableJpaRepository<InternalUserAccount> {
    InternalUserAccount findByUuid(String uuid);

    List<InternalUserAccount> findByRepositoryIdAndEmail(String repositoryId, String email);

    InternalUserAccount findByRepositoryIdAndConfirmationKey(String repositoryId, String key);

    List<InternalUserAccount> findByRealm(String realm);

    List<InternalUserAccount> findByRepositoryId(String repositoryId);

    List<InternalUserAccount> findByUserId(String userId);

    List<InternalUserAccount> findByUserIdAndRealm(String userId, String realm);

    List<InternalUserAccount> findByUserIdAndRepositoryId(String userId, String repositoryId);
}
