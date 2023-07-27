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

package it.smartcommunitylab.aac.openid.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface OIDCUserAccountRepository
    extends CustomJpaRepository<OIDCUserAccount, OIDCUserAccountId>, DetachableJpaRepository<OIDCUserAccount> {
    OIDCUserAccount findByUuid(String uuid);

    List<OIDCUserAccount> findByRepositoryIdAndEmail(String repositoryId, String email);

    List<OIDCUserAccount> findByRepositoryIdAndUsername(String repositoryId, String username);

    List<OIDCUserAccount> findByRealm(String realm);

    List<OIDCUserAccount> findByRepositoryId(String repositoryId);

    List<OIDCUserAccount> findByUserId(String userId);

    List<OIDCUserAccount> findByUserIdAndRealm(String userId, String realm);

    List<OIDCUserAccount> findByUserIdAndRepositoryId(String userId, String repositoryId);
}
