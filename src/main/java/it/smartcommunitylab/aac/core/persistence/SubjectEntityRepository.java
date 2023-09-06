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

package it.smartcommunitylab.aac.core.persistence;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectEntityRepository extends CustomJpaRepository<SubjectEntity, String> {
    SubjectEntity findBySubjectId(String subjectId);

    @Query(
        "select s from SubjectEntity as s where (s.subjectId = ?1 and s.type = '" + SystemKeys.RESOURCE_CLIENT + "')"
    )
    SubjectEntity findByClientId(String clientId);

    @Query("select s from SubjectEntity as s where (s.subjectId = ?1 and s.type = '" + SystemKeys.RESOURCE_USER + "')")
    SubjectEntity findByUserId(String userId);

    long countByRealm(String realm);

    List<SubjectEntity> findByRealm(String realm);

    List<SubjectEntity> findByRealmAndType(String realm, String type);

    List<SubjectEntity> findByRealmAndSubjectIdContainingIgnoreCaseOrRealmAndNameContainingIgnoreCase(
        String realms,
        String subjectId,
        String realmn,
        String name
    );
}
