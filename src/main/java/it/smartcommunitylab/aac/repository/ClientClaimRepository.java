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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.model.ClientClaim;

/**
 * 
 * @author raman
 *
 */
@Repository
public interface ClientClaimRepository extends CustomJpaRepository<ClientClaim, String> {

	@Query("select cc from ClientClaim cc where cc.client.clientId = ?1")
	List<ClientClaim> findByClient(String client);
	
	@Query(value="select distinct cc.client.clientId, cc.client.name from ClientClaim cc where cc.claim.service.serviceId = LOWER(?1)", 
			countQuery = "select count(distinct cc.client.clientId) from ClientClaim cc where cc.claim.service.serviceId = LOWER(?1)")
	Page<String[]> findClientDataByService(String serviceId, Pageable page);

	@Query("select cc from ClientClaim cc where cc.client.clientId = ?1 and cc.claim.service.serviceId = LOWER(?2)")
	List<ClientClaim> findByClientAndService(String clientId, String serviceId);

}
