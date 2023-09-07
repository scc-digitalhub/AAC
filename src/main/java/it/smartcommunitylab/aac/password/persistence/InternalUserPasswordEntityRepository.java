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

package it.smartcommunitylab.aac.password.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;
import java.util.List;

public interface InternalUserPasswordEntityRepository
    extends
        CustomJpaRepository<InternalUserPasswordEntity, String>, DetachableJpaRepository<InternalUserPasswordEntity> {
    List<InternalUserPasswordEntity> findByRepositoryId(String repositoryId);

    List<InternalUserPasswordEntity> findByRealm(String realm);

    List<InternalUserPasswordEntity> findByRepositoryIdAndUserId(String repositoryId, String userId);

    InternalUserPasswordEntity findByRepositoryIdAndUserIdAndStatusOrderByCreateDateDesc(
        String repositoryId,
        String userId,
        String status
    );

    InternalUserPasswordEntity findByRepositoryIdAndResetKey(String repositoryId, String key);
    // List<InternalUserPasswordEntity> findByRepositoryIdAndUsernameOrderByCreateDateDesc(
    //     String repositoryId,
    //     String username
    // );
}
