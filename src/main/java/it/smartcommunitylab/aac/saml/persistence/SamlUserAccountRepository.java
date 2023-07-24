package it.smartcommunitylab.aac.saml.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface SamlUserAccountRepository
    extends CustomJpaRepository<SamlUserAccount, SamlUserAccountId>, DetachableJpaRepository<SamlUserAccount> {
    SamlUserAccount findByUuid(String uuid);

    List<SamlUserAccount> findByRepositoryIdAndEmail(String repositoryId, String email);

    List<SamlUserAccount> findByRepositoryIdAndUsername(String repositoryId, String username);

    List<SamlUserAccount> findByRealm(String realm);

    List<SamlUserAccount> findByRepositoryId(String repositoryId);

    List<SamlUserAccount> findByUserId(String userId);

    List<SamlUserAccount> findByUserIdAndRealm(String userId, String realm);

    List<SamlUserAccount> findByUserIdAndRepositoryId(String userId, String repositoryId);
}
