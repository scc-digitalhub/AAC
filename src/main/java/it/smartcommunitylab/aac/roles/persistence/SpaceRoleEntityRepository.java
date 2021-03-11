package it.smartcommunitylab.aac.roles.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface SpaceRoleEntityRepository extends CustomJpaRepository<SpaceRoleEntity, Long> {

    List<SpaceRoleEntity> findBySubject(String subject);

    List<SpaceRoleEntity> findBySubjectAndContext(String subject, String context);

    List<SpaceRoleEntity> findBySubjectAndContextAndSpace(String subject, String context, String space);

    SpaceRoleEntity findBySubjectAndContextAndSpaceAndRole(String subject, String context, String space, String role);
}
