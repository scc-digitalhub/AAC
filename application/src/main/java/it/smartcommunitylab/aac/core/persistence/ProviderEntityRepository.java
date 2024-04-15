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

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface ProviderEntityRepository extends CustomJpaRepository<ProviderEntity, String> {
    ProviderEntity findByProvider(String provider);
    List<ProviderEntity> findByRealm(String realm);

    List<ProviderEntity> findByType(String type);
    List<ProviderEntity> findByTypeAndAuthority(String type, String authority);
    List<ProviderEntity> findByTypeAndRealm(String type, String realm);

    List<ProviderEntity> findByTypeAndAuthorityAndRealm(String type, String authority, String realm);

    Page<ProviderEntity> findByTypeAndRealm(String type, String realm, Pageable page);

    Page<ProviderEntity> findByTypeAndRealmAndNameContainingIgnoreCaseOrTypeAndRealmAndProviderContainingIgnoreCase(
        String typen,
        String realmn,
        String name,
        String typep,
        String realmp,
        String provider,
        Pageable page
    );
}
