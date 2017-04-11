/**
 *    Copyright 2012-2013 Trento RISE
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

import it.smartcommunitylab.aac.model.ClientDetailsEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
/**
 * Persistent repository of {@link ClientDetailsEntity} entities
 * @author raman
 *
 */
@Repository
public interface ClientDetailsRepository extends JpaRepository<ClientDetailsEntity, Long> {

	@Query("select c from ClientDetailsEntity c where c.developerId=?1")
	public List<ClientDetailsEntity> findByDeveloperId(Long developerId);
	
	@Query("select c from ClientDetailsEntity c where c.clientId=?1")
	public ClientDetailsEntity findByClientId(String clientId);
	
	@Query("select c from ClientDetailsEntity c where c.name=?1")
	public ClientDetailsEntity findByName(String clientId);	

}
//@Query("select u from User u left join u.attributeEntities a where a.authority.name=?1 and a.key=?2 and a.value=?3")