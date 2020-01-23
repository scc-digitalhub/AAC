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
package it.smartcommunitylab.aac.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.dto.ServiceDTO;
import it.smartcommunitylab.aac.model.Service;

/**
 * 
 * @author raman
 *
 */
@Repository
public interface ServiceModelRepository extends JpaRepository<Service, String> {

	@Query("select s from Service s where s.name like %?1%")
	Page<Service> findByName(String name, Pageable pageable);

	@Query("select s from Service s where s.context in ?1 or (?2 = true and s.context is null)")
	List<Service> findByContexts(Set<String> contexts, boolean withNull);

	@Query("select s from Service s where (s.context = ?1 or (?1 is null and s.context is null))")
	List<Service> findByContext(String context);

	@Query("select s from Service s where s.namespace = ?1")
	Service findByNamespace(String namespace);

	@Query("select ss.service.serviceId from ServiceScope ss where ss.scope in ?1")
	Set<String> findServiceIdsByScopes(Set<String> scopes);


}
