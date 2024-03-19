package it.smartcommunitylab.aac.spid.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpidUserAccountEntityRepository
    extends
        CustomJpaRepository<SpidUserAccountEntity, SpidUserAccountId>, DetachableJpaRepository<SpidUserAccountEntity> {
    SpidUserAccountEntity findByUuid(String uuid);

    List<SpidUserAccountEntity> findByRepositoryIdAndEmail(String repositoryId, String email);

    List<SpidUserAccountEntity> findByRepositoryIdAndUsername(String repositoryId, String username);

    List<SpidUserAccountEntity> findByRepositoryIdAndFiscalNumber(String repositoryId, String fiscalNumber);

    List<SpidUserAccountEntity> findByRepositoryIdAndSpidCode(String repositoryId, String spidCode);

    List<SpidUserAccountEntity> findByRealm(String realm);

    List<SpidUserAccountEntity> findByRepositoryId(String repositoryId);

    List<SpidUserAccountEntity> findByUserId(String userId);

    List<SpidUserAccountEntity> findByUserIdAndRealm(String userId, String realm);

    List<SpidUserAccountEntity> findByUserIdAndRepositoryId(String userId, String repositoryId);

}
