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

import it.smartcommunitylab.aac.model.ResourceParameter;
import it.smartcommunitylab.aac.model.ServiceDescriptor;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Persistent repository of {@link ResourceParameter} entities
 * @author raman
 *
 */
@Repository
public interface ResourceParameterRepository extends JpaRepository<ResourceParameter, Long> {

	
	@Query("select r from ResourceParameter r where r.clientId=?1")
	List<ResourceParameter> findByClientId(String clientId);
	
	@Query("select r from ResourceParameter r where r.service=?1")
	List<ResourceParameter>  findByService(ServiceDescriptor s);
//	List<ResourceParameter> findByClientIdAndServiceId(String clientId, String serviceId);
//	List<ResourceParameter> findByClientIdAndResourceId(String clientId, String resourceId);
//
//	List<ResourceParameter> findByResourceId(String resourceId);
//	List<ResourceParameter> findByServiceId(String serviceId);
	
	@Query("select r from ResourceParameter r where r.service=?1 and r.parameter=?2")
	List<ResourceParameter> findByServiceAndParameter(ServiceDescriptor service, String parameter);
}
