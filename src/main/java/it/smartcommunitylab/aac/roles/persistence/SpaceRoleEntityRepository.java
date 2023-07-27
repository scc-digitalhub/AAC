/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.roles.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SpaceRoleEntityRepository extends CustomJpaRepository<SpaceRoleEntity, Long> {
    List<SpaceRoleEntity> findBySubject(String subject);

    @Query("select r from SpaceRoleEntity r where subject = ?1 and (context = ?2 or context is null and ?2 is null)")
    List<SpaceRoleEntity> findBySubjectAndContext(String subject, String context);

    @Query(
        "select r from SpaceRoleEntity r where subject = ?1 and (context = ?2 or context is null and ?2 is null) and (space = ?3 or (space is null or space = '') and ?3 is null)"
    )
    List<SpaceRoleEntity> findBySubjectAndContextAndSpace(String subject, String context, String space);

    @Query(
        "select r from SpaceRoleEntity r where subject = ?1 and (context = ?2 or context is null and ?2 is null) and (space = ?3 or (space is null or space = '') and ?3 is null) and role = ?4"
    )
    SpaceRoleEntity findBySubjectAndContextAndSpaceAndRole(String subject, String context, String space, String role);

    @Query(
        "select distinct r.subject from SpaceRoleEntity r where (context = ?1 or context is null and ?1 is null) and (space = ?2 or (space is null or space = '') and ?2 is null)"
    )
    Page<String> findByContextAndSpace(String context, String space, Pageable pageRequest);

    @Query(
        "select distinct r.subject from SpaceRoleEntity r where (context = ?1 or context is null and ?1 is null) and (space = ?2 or (space is null or space = '') and ?2 is null) and subject like concat('%', ?2,'%')"
    )
    Page<String> findByContextAndSpaceAndSubject(String context, String space, String q, Pageable pageRequest);
}
