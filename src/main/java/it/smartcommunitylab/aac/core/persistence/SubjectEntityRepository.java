package it.smartcommunitylab.aac.core.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface SubjectEntityRepository extends CustomJpaRepository<SubjectEntity, String> {

    SubjectEntity findBySubjectId(String subjectId);

    @Query("select s from SubjectEntity as s where (s.subjectId = ?1 and s.type = '" + SystemKeys.RESOURCE_CLIENT
            + "')")
    SubjectEntity findByClientId(String clientId);

    @Query("select s from SubjectEntity as s where (s.subjectId = ?1 and s.type = '" + SystemKeys.RESOURCE_USER + "')")
    SubjectEntity findByUserId(String userId);

    long countByRealm(String realm);

    List<SubjectEntity> findByRealm(String realm);

    List<SubjectEntity> findByRealmAndType(String realm, String type);

    List<SubjectEntity> findByRealmAndSubjectIdContainingIgnoreCaseOrRealmAndNameContainingIgnoreCase(
            String realms, String subjectId,
            String realmn, String name);

}
