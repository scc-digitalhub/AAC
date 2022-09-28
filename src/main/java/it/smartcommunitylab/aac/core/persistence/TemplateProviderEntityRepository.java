package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface TemplateProviderEntityRepository extends CustomJpaRepository<TemplateProviderEntity, String> {

    TemplateProviderEntity findByRealm(String realm);

    List<TemplateProviderEntity> findByAuthority(String authority);

}
