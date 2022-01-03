package it.smartcommunitylab.aac.roles.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface SpaceRoleEntityRepository extends CustomJpaRepository<SpaceRoleEntity, Long> {

    List<SpaceRoleEntity> findBySubject(String subject);

    @Query("select r from SpaceRoleEntity r where subject = ?1 and (context = ?2 or context is null and ?2 is null)")
    List<SpaceRoleEntity> findBySubjectAndContext(String subject, String context);

    @Query("select r from SpaceRoleEntity r where subject = ?1 and (context = ?2 or context is null and ?2 is null) and (space = ?3 or (space is null or space = '') and ?3 is null)")
    List<SpaceRoleEntity> findBySubjectAndContextAndSpace(String subject, String context, String space);

    @Query("select r from SpaceRoleEntity r where subject = ?1 and (context = ?2 or context is null and ?2 is null) and (space = ?3 or (space is null or space = '') and ?3 is null) and role = ?4")
    SpaceRoleEntity findBySubjectAndContextAndSpaceAndRole(String subject, String context, String space, String role);
    
    @Query("select distinct r.subject from SpaceRoleEntity r where (context = ?1 or context is null and ?1 is null) and (space = ?2 or (space is null or space = '') and ?2 is null)")
    Page<String> findByContextAndSpace(String context, String space, Pageable pageRequest);
    
    @Query("select distinct r.subject from SpaceRoleEntity r where (context = ?1 or context is null and ?1 is null) and (space = ?2 or (space is null or space = '') and ?2 is null) and subject like concat('%', ?2,'%')")
    Page<String> findByContextAndSpaceAndSubject(String context, String space, String q, Pageable pageRequest);
}
