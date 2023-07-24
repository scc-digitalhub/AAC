package it.smartcommunitylab.aac.oauth.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuth2ClientEntityRepository
    extends CustomJpaRepository<OAuth2ClientEntity, Long>, DetachableJpaRepository<OAuth2ClientEntity> {
    OAuth2ClientEntity findByClientId(String clientId);
}
