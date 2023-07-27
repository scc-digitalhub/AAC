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
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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
