package it.smartcommunitylab.aac.core.persistence;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface UserEntityRepository extends CustomJpaRepository<UserEntity, Long> {

    UserEntity findByUuid(String uuid);
}
