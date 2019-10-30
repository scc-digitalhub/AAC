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

import it.smartcommunitylab.aac.model.Resource;
import it.smartcommunitylab.aac.model.ResourceParameter;
import it.smartcommunitylab.aac.model.ServiceDescriptor;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 
 * @author raman
 *
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

	@Query("select r from Resource r where r.resourceUri=?1")
	public Resource findByResourceUri(String resourceUri);
	
	@Query("select r from Resource r where r.clientId=?1")
	public List<Resource> findByClientId(String clientId);
	
	@Query("select r from Resource r where r.resourceParameter=?1")
	public List<Resource> findByResourceParameter(ResourceParameter rpdb);
	
	@Query("select r from Resource r where r.service=?1")
	public List<Resource> findByService(ServiceDescriptor s);
	
	@Query("select r from Resource r where r.service=?1 and r.resourceType=?2")
	public List<Resource> findByServiceAndResourceType(ServiceDescriptor service, String resourceType);
	
	@Query("select r from Resource r where r.service.serviceId=?1 and r.resourceType=?2")
	public Resource findByServiceIdAndResourceType(String serviceId, String resourceType);	
	
}
