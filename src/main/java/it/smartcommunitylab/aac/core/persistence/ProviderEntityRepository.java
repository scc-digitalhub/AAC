package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.data.repository.NoRepositoryBean;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@NoRepositoryBean
public interface ProviderEntityRepository<P extends ProviderEntity> extends CustomJpaRepository<P, String> {

    P findByProvider(String provider);

    List<P> findByAuthority(String authority);

    List<P> findByRealm(String realm);

    List<P> findByAuthorityAndRealm(String authority, String realm);

}
