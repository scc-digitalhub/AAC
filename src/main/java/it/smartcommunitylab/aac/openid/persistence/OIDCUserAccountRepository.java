package it.smartcommunitylab.aac.openid.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface OIDCUserAccountRepository
        extends CustomJpaRepository<OIDCUserAccount, OIDCUserAccountId>, DetachableJpaRepository<OIDCUserAccount> {

    OIDCUserAccount findByUuid(String uuid);

    List<OIDCUserAccount> findByRepositoryIdAndEmail(String repositoryId, String email);

    List<OIDCUserAccount> findByRepositoryIdAndUsername(String repositoryId, String username);

    List<OIDCUserAccount> findByRealm(String realm);

    List<OIDCUserAccount> findByRepositoryId(String repositoryId);

    List<OIDCUserAccount> findByUserId(String userId);

    List<OIDCUserAccount> findByUserIdAndRealm(String userId, String realm);

    List<OIDCUserAccount> findByUserIdAndRepositoryId(String userId, String repositoryId);

}