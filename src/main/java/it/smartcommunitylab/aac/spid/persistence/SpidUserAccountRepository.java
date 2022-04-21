package it.smartcommunitylab.aac.spid.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface SpidUserAccountRepository extends
        CustomJpaRepository<SpidUserAccount, SpidUserAccountId>, DetachableJpaRepository<SpidUserAccount> {

    SpidUserAccount findByProviderAndUuid(String provider, String uuid);

    List<SpidUserAccount> findByProviderAndSpidCode(String provider, String spidCode);

    List<SpidUserAccount> findByProviderAndEmail(String provider, String email);

    List<SpidUserAccount> findByProviderAndUsername(String provider, String username);

    List<SpidUserAccount> findByProviderAndPhone(String provider, String phone);

    List<SpidUserAccount> findByProviderAndFiscalNumber(String provider, String fiscalNumber);

    List<SpidUserAccount> findByProviderAndIvaCode(String provider, String ivaCode);

    List<SpidUserAccount> findByRealm(String realm);

    List<SpidUserAccount> findByProvider(String provider);

    List<SpidUserAccount> findByUserIdAndRealm(String userId, String realm);

    List<SpidUserAccount> findByUserIdAndProvider(String userId, String provider);

}