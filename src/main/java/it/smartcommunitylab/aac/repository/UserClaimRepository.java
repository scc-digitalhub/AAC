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

import it.smartcommunitylab.aac.model.UserClaim;

/**
 * 
 * @author raman
 *
 */
@Repository
public interface UserClaimRepository extends JpaRepository<UserClaim, Long> {
	
	@Query("select uc from UserClaim uc where uc.user.id = ?1")
	List<UserClaim> findByUser(Long userId);

	@Query(value="select distinct uc.user.id, uc.username from UserClaim uc where uc.claim.service.serviceId = LOWER(?1) and uc.username LIKE %?2%", 
			countQuery = "select count(distinct uc.username) from UserClaim uc where uc.claim.service.serviceId = LOWER(?1) and uc.username LIKE %?2%")
	Page<Object[]> findUserDataByService(String serviceId, String name, Pageable page);

	@Query("select uc from UserClaim uc where uc.user.id = ?1 and uc.claim.service.serviceId = LOWER(?2)")
	List<UserClaim> findByUserAndService(Long userId, String serviceId);

	@Query("select uc from UserClaim uc where uc.username = LOWER(?1) and uc.user is null")
	List<UserClaim> findByUsername(String username);
	
	@Query("select uc from UserClaim uc where uc.username = LOWER(?1) and uc.claim.service.serviceId = LOWER(?2)")
	List<UserClaim> findByUsernameAndService(String username, String serviceId);

}
