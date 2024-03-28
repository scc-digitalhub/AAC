/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface SpidUserAccountEntityRepository
    extends
        CustomJpaRepository<SpidUserAccountEntity, SpidUserAccountId>, DetachableJpaRepository<SpidUserAccountEntity> {
    SpidUserAccountEntity findByUuid(String uuid);

    List<SpidUserAccountEntity> findByRepositoryIdAndEmail(String repositoryId, String email);

    List<SpidUserAccountEntity> findByRepositoryIdAndUsername(String repositoryId, String username);

    List<SpidUserAccountEntity> findByRepositoryIdAndFiscalNumber(String repositoryId, String fiscalNumber);

    List<SpidUserAccountEntity> findByRepositoryIdAndSpidCode(String repositoryId, String spidCode);

    List<SpidUserAccountEntity> findByRealm(String realm);

    List<SpidUserAccountEntity> findByRepositoryId(String repositoryId);

    List<SpidUserAccountEntity> findByUserId(String userId);

    List<SpidUserAccountEntity> findByUserIdAndRealm(String userId, String realm);

    List<SpidUserAccountEntity> findByUserIdAndRepositoryId(String userId, String repositoryId);
}
