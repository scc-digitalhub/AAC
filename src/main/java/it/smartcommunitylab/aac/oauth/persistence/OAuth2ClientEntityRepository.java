package it.smartcommunitylab.aac.oauth.persistence;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface OAuth2ClientEntityRepository
        extends CustomJpaRepository<OAuth2ClientEntity, Long>, DetachableJpaRepository<OAuth2ClientEntity> {

    OAuth2ClientEntity findByClientId(String clientId);

}
