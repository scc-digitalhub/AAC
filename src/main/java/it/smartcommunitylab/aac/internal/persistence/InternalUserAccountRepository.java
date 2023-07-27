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
public interface InternalUserAccountRepository
    extends
        CustomJpaRepository<InternalUserAccount, InternalUserAccountId>, DetachableJpaRepository<InternalUserAccount> {
    InternalUserAccount findByUuid(String uuid);

    List<InternalUserAccount> findByRepositoryIdAndEmail(String repositoryId, String email);

    InternalUserAccount findByRepositoryIdAndConfirmationKey(String repositoryId, String key);

    List<InternalUserAccount> findByRealm(String realm);

    List<InternalUserAccount> findByRepositoryId(String repositoryId);

    List<InternalUserAccount> findByUserId(String userId);

    List<InternalUserAccount> findByUserIdAndRealm(String userId, String realm);

    List<InternalUserAccount> findByUserIdAndRepositoryId(String userId, String repositoryId);
}
