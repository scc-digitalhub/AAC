package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface SubjectAuthorityEntityRepository extends CustomJpaRepository<SubjectAuthorityEntity, Long> {

    List<SubjectAuthorityEntity> findBySubject(String subject);

    List<SubjectAuthorityEntity> findBySubjectAndRealm(String subject, String realm);
    
    List<SubjectAuthorityEntity> findByRealm(String realm);

    List<SubjectAuthorityEntity> findByRealmAndRole(String realm, String role);

}
