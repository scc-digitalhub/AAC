package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface RoleEntityRepository extends CustomJpaRepository<RoleEntity, Long> {

    List<RoleEntity> findBySubject(String subject);

    List<RoleEntity> findBySubjectAndContext(String subject, String context);

    List<RoleEntity> findBySubjectAndContextAndSpace(String subject, String context, String space);

    RoleEntity findBySubjectAndContextAndSpaceAndRole(String subject, String context, String space, String role);
}
