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

package it.smartcommunitylab.aac.clients.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientEntityRepository extends CustomJpaRepository<ClientEntity, String> {
    ClientEntity findByClientId(String clientId);

    List<ClientEntity> findByRealmAndName(String realm, String name);

    List<ClientEntity> findByRealm(String realm);

    List<ClientEntity> findByRealmAndType(String realm, String type);

    Page<ClientEntity> findByRealm(String realm, Pageable page);

    Page<ClientEntity> findByRealmAndNameContainingIgnoreCase(String realm, String name, Pageable page);

    Page<ClientEntity> findByRealmAndNameContainingIgnoreCaseOrRealmAndClientIdContainingIgnoreCase(
        String realmn,
        String name,
        String realmc,
        String clientId,
        Pageable page
    );

    long countByRealm(String realm);
}
