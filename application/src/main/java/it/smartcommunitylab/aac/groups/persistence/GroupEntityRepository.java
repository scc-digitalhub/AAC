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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * @author raman
 *
 */
@Repository
public interface GroupEntityRepository
    extends CustomJpaRepository<GroupEntity, String>, JpaSpecificationExecutor<GroupEntity> {
    GroupEntity findByRealmAndGroup(String realm, String group);

    List<GroupEntity> findByRealm(String realm);

    Page<GroupEntity> findByRealm(String realm, Pageable pageRequest);

    List<GroupEntity> findByRealmAndParentGroup(String realm, String parentGroup);
}
