package it.smartcommunitylab.aac.roles.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface SubjectRoleEntityRepository extends CustomJpaRepository<SubjectRoleEntity, Long> {

    List<SubjectRoleEntity> findBySubject(String subject);

    List<SubjectRoleEntity> findBySubjectAndRealm(String subject, String realm);

    List<SubjectRoleEntity> findByRealmAndRole(String realm, String role);

}
