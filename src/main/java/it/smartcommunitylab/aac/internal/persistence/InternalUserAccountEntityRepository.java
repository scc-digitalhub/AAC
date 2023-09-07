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

package it.smartcommunitylab.aac.internal.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface InternalUserAccountEntityRepository
    extends
        CustomJpaRepository<InternalUserAccountEntity, InternalUserAccountId>,
        DetachableJpaRepository<InternalUserAccountEntity> {
    InternalUserAccountEntity findByUuid(String uuid);
    InternalUserAccountEntity findByRepositoryIdAndConfirmationKey(String repositoryId, String key);

    List<InternalUserAccountEntity> findByRepositoryIdAndEmail(String repositoryId, String email);
    List<InternalUserAccountEntity> findByRepositoryIdAndUsername(String repositoryId, String username);

    List<InternalUserAccountEntity> findByRealm(String realm);

    List<InternalUserAccountEntity> findByRepositoryId(String repositoryId);

    List<InternalUserAccountEntity> findByUserId(String userId);

    List<InternalUserAccountEntity> findByUserIdAndRealm(String userId, String realm);

    List<InternalUserAccountEntity> findByUserIdAndRepositoryId(String userId, String repositoryId);
}
