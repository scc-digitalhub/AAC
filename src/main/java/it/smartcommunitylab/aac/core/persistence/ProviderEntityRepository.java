package it.smartcommunitylab.aac.core.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import java.util.List;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ProviderEntityRepository<P extends ProviderEntity> extends CustomJpaRepository<P, String> {
    P findByProvider(String provider);

    List<P> findByAuthority(String authority);

    List<P> findByRealm(String realm);

    List<P> findByAuthorityAndRealm(String authority, String realm);
}
