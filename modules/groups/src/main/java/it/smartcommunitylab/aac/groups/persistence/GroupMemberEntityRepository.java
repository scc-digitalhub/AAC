/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.groups.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author raman
 *
 */
@Repository
public interface GroupMemberEntityRepository
    extends CustomJpaRepository<GroupMemberEntity, Long>, JpaSpecificationExecutor<GroupMemberEntity> {
    GroupMemberEntity findByRealmAndGroupAndSubject(String realm, String group, String subject);

    long countByRealmAndGroup(String realm, String group);

    List<GroupMemberEntity> findByRealmAndGroup(String realm, String group);

    Page<GroupMemberEntity> findByRealmAndGroup(String realm, String group, Pageable pageRequest);

    List<GroupMemberEntity> findByRealmAndGroupIn(String realm, Set<String> groups);

    List<GroupMemberEntity> findBySubject(String subject);

    List<GroupMemberEntity> findBySubjectAndRealm(String subject, String realm);
    //    @Query("select g from GroupMemberEntity gm inner join GroupEntity g on gm.group = g.uuid where gm.subject = ?1")
    //    List<GroupEntity> findGroupsBySubject(String subject);

}
