package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface UserRoleEntityRepository extends CustomJpaRepository<UserRoleEntity, Long> {

    List<UserRoleEntity> findBySubject(String subject);

    List<UserRoleEntity> findBySubjectAndRealm(String subject, String realm);
}
