/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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
 */
package it.smartcommunitylab.aac.services.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

/**
 * 
 * @author raman
 *
 */
@Repository
public interface ServiceEntityRepository extends CustomJpaRepository<ServiceEntity, String> {

    ServiceEntity findByRealmAndNamespace(String realm, String namespace);

    ServiceEntity findByRealmAndResource(String realm, String resource);

    List<ServiceEntity> findByRealm(String realm);

    List<ServiceEntity> findByNameContainingIgnoreCase(String name);

    @Query("select namespace from #{#entityName} where realm = ?1")
    List<String> listAllNamespacesByRealm(String realm);

    @Query("select namespace from #{#entityName} where realm = ?1")
    List<String> listAllResourcesByRealm(String realm);
}
