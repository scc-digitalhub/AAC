package it.smartcommunitylab.aac.roles.persistence;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface SubjectRoleEntityRepository extends CustomJpaRepository<SubjectRoleEntity, Long> {

    SubjectRoleEntity findByRealmAndRoleAndSubject(String realm, String role, String subject);

    long countByRealmAndRole(String realm, String role);

    List<SubjectRoleEntity> findByRealmAndRole(String realm, String role);

    Page<SubjectRoleEntity> findByRealmAndRole(String realm, String role, Pageable pageRequest);

    List<SubjectRoleEntity> findByRealmAndRoleIn(String realm, Set<String> roles);

    List<SubjectRoleEntity> findBySubject(String subject);

    List<SubjectRoleEntity> findBySubjectAndRealm(String subject, String realm);

}
