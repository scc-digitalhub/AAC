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

package it.smartcommunitylab.aac.users.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEntityRepository
    extends CustomJpaRepository<UserEntity, String>, JpaSpecificationExecutor<UserEntity> {
    UserEntity findByUuid(String uuid);

    List<UserEntity> findByRealm(String realm);

    List<UserEntity> findByRealmAndUsername(String realm, String username);

    List<UserEntity> findByRealmAndEmailAddress(String realm, String emailAddress);

    //    Page<UserEntity> findByRealm(String realm, Pageable pageRequest);
    //    @Query("select u from UserEntity u where u.realm = ?1 and LOWER(u.username) like lower(concat('%', ?2,'%'))")
    //    Page<UserEntity> findByRealm(String realm, String q, Pageable pageRequest);

    //    @Query("select distinct u from UserEntity u left outer join UserRoleEntity r on u.uuid = r.subject where (r.realm = ?1 or u.realm = ?1)")
    Page<UserEntity> findByRealm(String realm, Pageable pageRequest);

    //    @Query("select distinct u from UserEntity u  left outer join UserRoleEntity r on u.uuid = r.subject where (r.realm = ?1 or u.realm = ?1) and LOWER(u.username) like lower(concat('%', ?2,'%'))")
    Page<UserEntity> findByRealmAndUsernameContainingIgnoreCase(String realm, String q, Pageable pageRequest);

    Page<
        UserEntity
    > findByRealmAndUsernameContainingIgnoreCaseOrRealmAndUuidContainingIgnoreCaseOrRealmAndEmailAddressContainingIgnoreCase(
        String realmn,
        String name,
        String realmu,
        String uuid,
        String realme,
        String email,
        Pageable page
    );

    long countByRealm(String realm);
}
