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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.model.ServiceClaim;

/**
 * 
 * @author raman
 *
 */
@Repository
public interface ServiceClaimRepository extends JpaRepository<ServiceClaim, Long> {

	@Query("select s from ServiceClaim s where s.service.serviceId = ?1")
	List<ServiceClaim> findByService(String serviceId);

	@Query("select s from ServiceClaim s where s.service.serviceId = ?1 and claim = ?2")
	ServiceClaim findByServiceAndClaim(String serviceId, String claim);

}
