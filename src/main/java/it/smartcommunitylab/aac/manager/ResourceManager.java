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

package it.smartcommunitylab.aac.manager;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.AUTHORITY;
import it.smartcommunitylab.aac.Config.RESOURCE_VISIBILITY;
import it.smartcommunitylab.aac.common.PatternMatcher;
import it.smartcommunitylab.aac.common.ResourceException;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.jaxbmodel.ResourceDeclaration;
import it.smartcommunitylab.aac.jaxbmodel.ResourceMapping;
import it.smartcommunitylab.aac.jaxbmodel.Service;
import it.smartcommunitylab.aac.jaxbmodel.Services;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Resource;
import it.smartcommunitylab.aac.model.ResourceParameter;
import it.smartcommunitylab.aac.model.ServiceDescriptor;
import it.smartcommunitylab.aac.oauth.ResourceStorage;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.ResourceParameterRepository;
import it.smartcommunitylab.aac.repository.ResourceRepository;
import it.smartcommunitylab.aac.repository.ServiceRepository;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityNotFoundException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriTemplate;

/**
 * Class used to operate resource model.
 * @author raman
 *
 */
@Component
@Transactional
public class ResourceManager {

	private static Log logger = LogFactory.getLog(ResourceManager.class);
	@Autowired
	private ResourceStorage resourceStorage;
	@Autowired
	private ResourceParameterRepository resourceParameterRepository;
	@Autowired
	private ResourceRepository resourceRepository;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	@Autowired
	private ServiceRepository serviceRepository;
	
	public void init() throws ResourceException {
		List<Service> services = loadResourceTemplates();
		processServiceObjects(services, null);
	}

	/**
	 * Save resource parameter. Check the uniqueness of the parameter value across all the
	 * parameters with the same resource parameter definition ID. Instantiate 
	 * all the derived resources. 
	 * @param rp
	 */
	public void storeResourceParameter(ResourceParameter rp, String serviceId) {
		validateResourceParameterData(rp);
		ResourceParameter rpold = rp.getId() == null ? null : resourceParameterRepository.findOne(rp.getId());
		// check uniqueness
		String clientId = rp.getClientId();
		if (rpold != null && !clientId.equals(clientId)) {
			throw new IllegalArgumentException("A parameter already used by another app");
		} else if (rpold == null) {
			ServiceDescriptor sd = serviceRepository.findOne(serviceId);
			if (sd == null) {
				throw new IllegalArgumentException("Unknown service: "+serviceId);
			} else {
				Service s = Utils.toServiceObject(sd);
				boolean found = false;
				for (ResourceDeclaration rd : s.getResource()) {
					if (rd.getId().equals(rp.getParameter())) {
						found = true; break;
					}
				}
				if (!found) {
					throw new IllegalArgumentException("Unknown parameter '"+rp.getParameter()+"' for service: "+serviceId);
				}
				rp.setService(sd);
			}
			
			resourceParameterRepository.save(rp);
			// derived resources
			Map<String, ResourceMapping> mappings = findResourceURIs(rp);
			// store new resources entailed by the resource parameter
			if (mappings != null) {
				Set<String> newSet = new HashSet<String>();
				Set<String> newScopes = new HashSet<String>();
				for (String uri : mappings.keySet()) {
					ResourceMapping resourceMapping = mappings.get(uri);

					Resource r = prepareResource(clientId, rp,  uri, resourceMapping, rp.getVisibility());
					r.setService(sd);
					resourceRepository.save(r);
					newSet.add(r.getResourceId().toString());
					newScopes.add(r.getResourceUri());
				}
				// add automatically the resources entailed by own resource parameters to the client resourceIds
				ClientDetailsEntity cd = clientDetailsRepository.findByClientId(clientId);
				Set<String> oldSet = cd.getResourceIds();
				if (oldSet != null) newSet.addAll(oldSet);
				cd.setResourceIds(StringUtils.collectionToCommaDelimitedString(newSet));
				// add automatically the resources entailed by own resource parameters to the client scope
				oldSet = cd.getScope();
				if (oldSet != null) newScopes.addAll(oldSet);
				cd.setScope(StringUtils.collectionToCommaDelimitedString(newScopes));
				clientDetailsRepository.save(cd);
			}
		} else {
			throw new IllegalArgumentException("A parameter already exists");
		}
	}

	/**
	 * Validate parameter data
	 * @param rp
	 */
	private void validateResourceParameterData(ResourceParameter rp) {
		if (!StringUtils.hasText(rp.getValue())) {
			throw new IllegalArgumentException("empty parameter value");
		}
	}

	/**
	 * Update the visibility of the resource parameter. This only affects the child resources
	 * in case of more restrictive policy applied. If the changes are in conflict with the
	 * clients that use the corresponding resources, an exception is thrown. Also, if the new
	 * visibility is more relaxing than that of the parent, the exception is thrown.
	 * 
	 * @param id id of the changed resource parameter
	 * @param visibility new visibility value
	 * @return changed parameter
	 */
	public ResourceParameter updateResourceParameterVisibility(Long id, RESOURCE_VISIBILITY visibility) {
		assert visibility != null;
		ResourceParameter rpdb = resourceParameterRepository.findOne(id);
		if (rpdb != null) {
			String clientId = rpdb.getClientId();
			ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
			
			Map<String, RESOURCE_VISIBILITY> visibilityMap = new HashMap<String, Config.RESOURCE_VISIBILITY>();
			rpdb.setVisibility(visibility);
			for (String uri : findResourceURIs(rpdb).keySet()) {
				visibilityMap.put(uri, visibility);
				Resource res = resourceRepository.findByResourceUri(uri);
				res.setVisibility(visibility);
				resourceRepository.save(res);
			}
			
			if (!checkUsages(visibilityMap, clientId, client.getDeveloperId())) {
				throw new IllegalArgumentException("Resource is in use, cannot reduce visibility");
			}
			
			List<Resource> resources = resourceRepository.findByResourceParameter(rpdb);//.findByClientId(clientId);
			if (resources != null) {
				for (Resource r : resources) {
					r.setVisibility(rpdb.getVisibility());
				}
			}
			
		} else {
			throw new IllegalArgumentException("No resource parameter found");
		}
		return rpdb;
	}
	
	/**
	 * Check the visibility of the specified resources managed by the specified client and its owner by all the clients registered
	 * @param visibilityMap contains the resource URI and its target visibility
	 * @return true if at least one of the specified resources violates visibility constraint
	 */
	private boolean checkUsages(Map<String, RESOURCE_VISIBILITY> visibilityMap, String clientId, Long developerId) {
		List<ClientDetailsEntity> clients = clientDetailsRepository.findAll();
		for (ClientDetailsEntity client : clients) {
			// owned resources are visible, skip
			if (clientId.equals(client.getClientId())) continue;
			// 
			Set<String> uris = client.getScope();
			for (String uri : visibilityMap.keySet()) {
				if (uris.contains(uri)) {
					switch (visibilityMap.get(uri)) {
					// if should be visible only by the owning client app - violation
					case CLIENT_APP:
						return false;
						// if should be visible only by the owning developer - violation
					case DEVELOPER:
						if (!client.getDeveloperId().equals(developerId)) {
							return false;
						}
					default:
						break;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Try to remove the resource parameter and its children. Operation is recursive.
	 * If one of the derived resources is already in use, an exception is thrown.
	 * @param rpId
	 */
	public void removeResourceParameter(Long rpId) {
		// main parameter
		ResourceParameter rpdb = resourceParameterRepository.findOne(rpId);
		if (rpdb != null) {
			String clientId = rpdb.getClientId();
			Set<String> ids = new HashSet<String>();
			Set<String> scopes = new HashSet<String>();
			// aggregate all derived resource uris
			Collection<String> uris = findResourceURIs(rpdb).keySet();
			for (String uri : uris) {
				Resource r = resourceRepository.findByResourceUri(uri);
				if (r != null) {
					ids.add(r.getResourceId().toString());
					scopes.add(r.getResourceUri());
				}
			}
			ClientDetailsEntity owner = null;
			// check the resource uri usages
			for (ClientDetailsEntity cd : clientDetailsRepository.findAll()) {
				if (cd.getClientId().equals(clientId)) {
					owner = cd;
					continue;
				}
				if (!Collections.disjoint(cd.getResourceIds(), ids)) {
					throw new IllegalArgumentException("Resource is in use by other client app.");
				}
			} 
			// delete main and its children
			for (String id : ids){
				resourceRepository.delete(Long.parseLong(id));
			}
			if (owner != null) {
				Set<String> oldScopes = new HashSet<String>(owner.getScope());
				oldScopes.removeAll(scopes);
				owner.setScope(StringUtils.collectionToCommaDelimitedString(oldScopes));
				Set<String> oldIds = new HashSet<String>(owner.getResourceIds());
				oldIds.removeAll(ids);
				owner.setResourceIds(StringUtils.collectionToCommaDelimitedString(oldIds));
				clientDetailsRepository.save(owner);
				
			}
			resourceParameterRepository.delete(rpdb);
		}	
	}
	
	/**
	 * Find all the resource uris derived from the specified resource parameter and its parent parameters.
	 * @param rpdb
	 * @return map with URIs as the keys and mapping definitions as values.
	 */
	private Map<String,ResourceMapping> findResourceURIs(ResourceParameter rpdb) {
		Map<String, ResourceMapping> res = new HashMap<String, ResourceMapping>();
		Map<String,String> params = new HashMap<String, String>();
		params.put(rpdb.getParameter(), rpdb.getValue());
		// the service where parameter is defined
		ServiceDescriptor sd = rpdb.getService();
		if (sd == null) {
			throw new IllegalArgumentException("ServiceDescriptor is not found.");
		}
		Service s = Utils.toServiceObject(sd);
		// all the service resource mappings
		List<ResourceMapping> list = s.getResourceMapping();
		if (list != null) {
			for (ResourceMapping rm : list) {
				UriTemplate template = new UriTemplate(rm.getUri());
				// if the extracted parameters contain all the template parameters, the mapping is updated
				if (template.getVariableNames() != null) {
					if (new HashSet<String>(template.getVariableNames()).equals(params.keySet())) {
						URI uri = template.expand(params);
						res.put(uri.toString(), rm);
					}
				}
			}
		}
		
		return res;
	}

	/**
	 * Read resource parameters owned by the specified client, optionally restricting 
	 * to the specified service and resource parameter ID
	 * @param clientId
	 * @return
	 */
	public List<ResourceParameter> getOwnResourceParameters(String clientId) {
		if (clientId == null) {
			return Collections.emptyList();
		}
		return resourceParameterRepository.findByClientId(clientId);
	}
	
	/**
	 * Read the resources from the XML descriptor
	 * @return
	 */
	private List<Service> loadResourceTemplates() {
		try {
			JAXBContext jaxb = JAXBContext.newInstance(ServiceDescriptor.class, Services.class, ResourceMapping.class, ResourceDeclaration.class);
			Unmarshaller unm = jaxb.createUnmarshaller();
			JAXBElement<Services> element = (JAXBElement<Services>) unm
					.unmarshal(
							new StreamSource(getClass().getResourceAsStream(
									"resourceTemplates.xml")), Services.class);
			return element.getValue().getService();
		} catch (JAXBException e) {
			logger.error("Failed to load resource templates: "+e.getMessage(),e);
			return Collections.emptyList();
		}
	}

	private void processServiceObjects(List<Service> services, String ownerId) throws ResourceException {
		checkServiceListConsistency(services);

		List<ServiceDescriptor> dbServices = serviceRepository.findByOwnerId(ownerId);
		Map<String,ServiceDescriptor> dbServiceMap = new HashMap<String, ServiceDescriptor>();
		for (ServiceDescriptor s : dbServices) {
			dbServiceMap.put(s.getServiceId(), s); 
		}
		// split objects in created/updated/deleted
		Set<ServiceDescriptor> deleted  = new HashSet<ServiceDescriptor>();
		Map<Service,ServiceDescriptor> updated = new HashMap<Service,ServiceDescriptor>(); 
		Set<Service> created = new HashSet<Service>();
		Set<String> newOnes = new HashSet<String>();
		for (Service s : services) {
			if (!dbServiceMap.containsKey(s.getId())) {
				created.add(s);
			} else {
				updated.put(s, dbServiceMap.get(s.getId()));
			}
			newOnes.add(s.getId());
		}
		for (ServiceDescriptor s : dbServices) {
			if (!newOnes.contains(s.getServiceId())) {
				deleted.add(s);
			}
		}
		
		// process changes
		for (ServiceDescriptor s : deleted) {
			deleteService(s, ownerId);
		}
		for (Service service: updated.keySet()) {
			updateService(service, updated.get(service), ownerId);
		}
		for (Service service: created) {
			createService(service, ownerId);
		}
	}

	/**
	 * Check input service list consistency:
	 * <ul>
	 * <li> in the input list: </li>
	 * <li> check duplicate rm IDs in the service </li>
	 * <li> check duplicate r IDs in the service </li>
	 * <li> check matching URIs across services </li>
     * <li> with respect to DB data: </li>
	 * <li> check matching URIs across stored services (if matching, should be of the same service) </li>
     * </ul>
	 * @param services
	 * @throws ResourceException
	 */
	private void checkServiceListConsistency(List<Service> services) throws ResourceException {
		Map<String,String> uriServiceMap = new HashMap<String, String>();
		Set<ServiceKey> rmIDServiceSet = new HashSet<ServiceKey>();
		Set<ServiceKey> resIDServiceSet = new HashSet<ServiceKey>();
		for (Service s : services) {
			for (ResourceMapping rm: s.getResourceMapping()) {
				// check duplicate URIs in the list
				if (uriServiceMap.containsKey(rm.getUri())) {
					throw new ResourceException("Duplicate mapping URI in service resources: "+rm.getUri());
				}
				uriServiceMap.put(rm.getUri(), s.getId());
				// check duplicate mappings for the same service
				ServiceKey key = new ServiceKey(rm.getId(), s.getId());
				if (rmIDServiceSet.contains(key)) {
					throw new ResourceException("Duplicate mapping in service resources: "+rm.getId());
				}
				rmIDServiceSet.add(key);
			}
			for (ResourceDeclaration res: s.getResource()) {
				// check duplicate parameters for the same service
				ServiceKey key = new ServiceKey(res.getId(), s.getId());
				if (rmIDServiceSet.contains(key)) {
					throw new ResourceException("Duplicate resource parameter in service resources: "+res.getId());
				}
				resIDServiceSet.add(key);
			}
		}

		// read DB uri mappings
		Map<String,String> dbUriServiceMap = new HashMap<String, String>();
		List<ServiceDescriptor> dbServices = serviceRepository.findAll();
		for (ServiceDescriptor sd : dbServices) {
			Service s = Utils.toServiceObject(sd);
			for (ResourceMapping rm : s.getResourceMapping()) {
				dbUriServiceMap.put(rm.getUri(), s.getId());
			}
		}
		for (String inUri: uriServiceMap.keySet()) {
			// check if the same uri is already used by the same 
			if (dbUriServiceMap.containsKey(inUri)) {
				if (!dbUriServiceMap.get(inUri).equals(uriServiceMap.get(inUri))) {
					throw new ResourceException("Resource URI mapping '"+inUri+"' is in use by another service: "+dbUriServiceMap.get(inUri));
				}
				dbUriServiceMap.remove(inUri);
				continue;
			}
			for (String dbUri : dbUriServiceMap.keySet()) {
				if (new PatternMatcher(inUri, dbUri).compute()) {
					if (!dbUriServiceMap.get(dbUri).equals(uriServiceMap.get(inUri))) {
						throw new ResourceException("Resource URI mapping '"+inUri+"' matches another pattern '"+dbUri+"' of service: "+dbUriServiceMap.get(dbUri));
					}
				}
			}
		}
	}
	
	/**
	 * @param newService
	 * @param oldSd
	 * @param ownerId
	 * @return 
	 * @throws ResourceException 
	 */
	private Service updateService(Service newService, ServiceDescriptor oldSd, String ownerId) throws ResourceException {
		Service oldService = Utils.toServiceObject(oldSd);
		Map<String,ResourceMapping> oldMappings = new HashMap<String, ResourceMapping>();
		Map<String,ResourceDeclaration> oldParams = new HashMap<String, ResourceDeclaration>();
		for (ResourceMapping rm : oldService.getResourceMapping()) {
			oldMappings.put(rm.getId(), rm);
		}
		for (ResourceDeclaration rd : oldService.getResource()) {
			oldParams.put(rd.getId(), rd);
		}
		
		List<Resource> resourcesToCreate = new ArrayList<Resource>();
		List<Resource> resourcesToDelete = new ArrayList<Resource>();
		List<ResourceParameter> paramsToDelete = new ArrayList<ResourceParameter>();
		
		for (ResourceMapping rm : newService.getResourceMapping()) {
			// if a mapping is new or has the URI changed, mark it as new (or deleted/new)
			if (!oldMappings.containsKey(rm.getId()) ||
				!oldMappings.get(rm.getId()).getUri().equals(rm.getUri())) 
			{
				extractResources(rm, resourcesToCreate);
			// if remains the same, remove it from oldMappings so it will not be deleted
			} else {
				oldMappings.remove(rm.getId());
			}
		}
		for (ResourceDeclaration rd : newService.getResource()) {
			// if parameter exists, remove it from oldParams so it will not be deleted
			if (oldParams.containsKey(rd.getId())) {
				oldParams.remove(rd.getId());
			}
		}

		// extract resources to be deleted, if possible (not used)
		for (ResourceMapping rm : oldMappings.values()) {
			resourcesToDelete.addAll(resourceRepository.findByServiceAndResourceType(oldSd,rm.getId()));
		}
		// extract parameters to be deleted, if possible (not used)
		for (ResourceDeclaration rd : oldParams.values()) {
			paramsToDelete.addAll(resourceParameterRepository.findByServiceAndParameter(oldSd,rd.getId()));
		}
		cleanServiceResources(resourcesToDelete, paramsToDelete);
		ServiceDescriptor service = Utils.toServiceEntity(newService);
		service.setOwnerId(ownerId);
		serviceRepository.save(service);
		storeServiceResources(service, resourcesToCreate);
		return newService;
	}

	/**
	 * @param s
	 * @param ownerId
	 * @throws ResourceException 
	 */
	private void deleteService(ServiceDescriptor s, String ownerId) throws ResourceException {
		List<Resource> resources = resourceRepository.findByService(s);
		List<ResourceParameter> parameters = resourceParameterRepository.findByService(s);
		// clean up unused resources and parameters 
		cleanServiceResources(resources, parameters);
		// delete service
		serviceRepository.delete(s);
	}

	/**
	 * Delete specified resources and parameters if not in use by some client
	 * @param resources
	 * @param parameters
	 * @throws ResourceException
	 */
	private void cleanServiceResources(List<Resource> resources, List<ResourceParameter> parameters) throws ResourceException {
		// check the service resources are in use by the clients
		if (resources != null && ! resources.isEmpty()) {
			Set<String> ids = new HashSet<String>();
			for (Resource r : resources) {
				ids.add(""+r.getResourceId());
			}
			List<ClientDetailsEntity> clients = clientDetailsRepository.findAll();
			for (ClientDetailsEntity c : clients) {
				if (!Collections.disjoint(ids, c.getResourceIds())) {
					throw new ResourceException("Resource in use by client: "+c.getClientId());
				}
			}
		}
		resourceRepository.delete(resources);
		resourceParameterRepository.delete(parameters);
	}
	
	/**
	 * Create new service for the specified owner
	 * @param service
	 * @param ownerId
	 * @return created {@link Service} object
	 * @throws ResourceException 
	 */
	private Service createService(Service service, String ownerId) throws ResourceException {
		ServiceDescriptor entity = Utils.toServiceEntity(service);
		entity.setOwnerId(ownerId);
		// saving new service
		entity = serviceRepository.save(entity);
		// read non-parametric service resources 
		List<Resource> resourcesToStore = extractResources(service);
		// store resources
		storeServiceResources(entity, resourcesToStore);
		return Utils.toServiceObject(entity); 
	}

	private void storeServiceResources(ServiceDescriptor sd, List<Resource> resourcesToStore)
			throws ResourceException {
		if (resourcesToStore != null && !resourcesToStore.isEmpty()) {
			for (Iterator<Resource> iterator = resourcesToStore.iterator(); iterator.hasNext();) {
				Resource r = iterator.next();
				Resource existing = resourceRepository.findByResourceUri(r.getResourceUri());
				// if resource already exists and belongs to a different service, throw an exception
				if (existing != null && !existing.getService().getServiceId().equals(sd.getServiceId())) {
					throw new ResourceException("resource not unique: "+r.getResourceUri());
				} else if (existing != null) {
					iterator.remove();
				} else {
					r.setService(sd);
				}
			}
			// store new non-parametric resources
			resourceStorage.storeResources(resourcesToStore);
		}
	}

	/**
	 * Update the specified service object
	 * @param s
	 * @param ownerId
	 * @return updated {@link Service} object
	 * @throws ResourceException 
	 */
	private Service updateService(Service s, String ownerId) throws ResourceException {
		checkServiceListConsistency(Collections.singletonList(s));
		ServiceDescriptor old = serviceRepository.findOne(s.getId());
		return updateService(s, old, ownerId);
	} 
	
	private List<Resource> extractResources(Service s) {
		List<Resource> resources = new ArrayList<Resource>();
		// process resource mappings
		if (s.getResourceMapping() != null) {
			for (ResourceMapping  rm : s.getResourceMapping()) {
				// extract resource mappings recursively
				extractResources(rm,resources);
			}
		}
		return resources;
	}

	
	/**
	 * Extract resource mappings recursively
	 * @param rm
	 * @param resources
	 */
	private void extractResources(ResourceMapping rm, List<Resource> resources) {
		// add non-parametric resources to the target list
		if (!isParametric(rm)) {
			resources.add(prepareResource(null, null, rm.getUri(),rm, RESOURCE_VISIBILITY.PUBLIC));
		}
	}

	/**
	 * @param rm
	 * @return true if the mapping definition is parametric to the service resource parameters
	 */
	private boolean isParametric(ResourceMapping rm) {
		UriTemplate template = new UriTemplate(rm.getUri());
		return template.getVariableNames() != null && template.getVariableNames().size() > 0;
	}

	/**
	 * @param clientId
	 * @param rp 
	 * @param uri
	 * @param rm
	 * @param visibility 
	 * @return {@link Resource} instance out of mapping, clientID, and resource URI.
	 */
	protected Resource prepareResource(String clientId, ResourceParameter rp, String uri, ResourceMapping rm, RESOURCE_VISIBILITY visibility) {
		Resource r = new Resource();
		r.setAccessibleByOthers(rm.isAccessibleByOthers());
		r.setApprovalRequired(rm.isApprovalRequired());
		r.setAuthority(AUTHORITY.valueOf(rm.getAuthority().value()));
		r.setClientId(clientId);
		r.setResourceParameter(rp);
		UriTemplate template = new UriTemplate(rm.getUri());
		Map<String,String> params = template.match(uri);
		template = new UriTemplate(rm.getDescription());
		try {
			r.setDescription(URLDecoder.decode(template.expand(params).toString(),"utf8"));
		} catch (UnsupportedEncodingException e) {
			r.setDescription(rm.getDescription());
		}
		template = new UriTemplate(rm.getName());
		try {
			r.setName(URLDecoder.decode(template.expand(params).toString(),"utf8"));
		} catch (UnsupportedEncodingException e) {
			r.setName(rm.getName());
		}
		r.setResourceType(rm.getId());
		r.setResourceUri(uri);
		r.setVisibility(visibility);
		return r;
	}

	/**
	 * @return
	 */
	public Service getServiceObject(String serviceId) {
		ServiceDescriptor service = serviceRepository.findOne(serviceId);
		return Utils.toServiceObject(service);
	}

	/**
	 * @return all the services
	 */
	public List<Service> getServiceObjects() {
		List<ServiceDescriptor> services = serviceRepository.findAll();
		List<Service> res = new ArrayList<Service>();
		for (ServiceDescriptor sd : services) {
			res.add(Utils.toServiceObject(sd));
		}
		return res;
	}

	/**
	 * @return all the services of the specified user
	 */
	public List<Service> getServiceObjects(String ownerId) {
		List<ServiceDescriptor> services = serviceRepository.findByOwnerId(ownerId);
		List<Service> res = new ArrayList<Service>();
		for (ServiceDescriptor sd : services) {
			res.add(Utils.toServiceObject(sd));
		}
		return res;
	}
	
	/**
	 * Save the specified {@link ServiceDescriptor} object, only the id/name/description fields
	 * @param service
	 * @param userId
	 * @return
	 */
	public Service saveServiceObject(Service service, Long userId) {
		validateServiceData(service);
		ServiceDescriptor sdOld = serviceRepository.findOne(service.getId()); 
		if (sdOld != null && ! sdOld.getOwnerId().equals(userId.toString())) {
			throw new SecurityException("Service ID is in use by another user");
		}
		ServiceDescriptor sd = sdOld == null ? Utils.toServiceEntity(service) : sdOld;
		sd.setDescription(service.getDescription());
		sd.setServiceName(service.getName());
		sd.setServiceId(service.getId());
		sd.setOwnerId(userId.toString());
		sd = serviceRepository.save(sd);
		return Utils.toServiceObject(sd);
	}

	
	
	/**
	 * Validate service fields
	 * @param service
	 */
	private void validateServiceData(Service service) {
		if (!StringUtils.hasText(service.getId())) {
			throw new IllegalArgumentException("empty service ID");
		}
		if (!StringUtils.hasText(service.getName())) {
			throw new IllegalArgumentException("empty service name");
		}
		if (!StringUtils.hasText(service.getDescription())) {
			throw new IllegalArgumentException("empty service description");
		}
	}

	/**
	 * Delete the specified service
	 * @param serviceId
	 * @param ownerId
	 * @throws ResourceException 
	 */
	public void deleteService(String serviceId, String ownerId) throws ResourceException {
		ServiceDescriptor old = serviceRepository.findOne(serviceId);
		deleteService(old, ownerId);
	}

	/**
	 * read the resource that the client app may request permissions for
	 * @param clientId
	 * @return
	 */
	public List<Resource> getAvailableResources(String clientId, Long userId) {
		if (clientId != null) {
			// check all the resources
			List<Resource> list = resourceRepository.findAll();
			for (Iterator<Resource> iterator = list.iterator(); iterator.hasNext();) {
				Resource resource = iterator.next();
				// interested only in non-owned resources
				if (!clientId.equals(resource.getClientId()) && resource.getClientId() != null) {
					if (!resource.isAccessibleByOthers()) {
						iterator.remove();
						continue;
					}
					// check the resource visibility for the current client
					switch (resource.getVisibility()) {
					case CLIENT_APP:
						iterator.remove();
						break;
					case DEVELOPER:
						ClientDetailsEntity cd = clientDetailsRepository.findByClientId(resource.getClientId());
						if (cd == null || !cd.getDeveloperId().equals(userId)) {
							iterator.remove();
						}
					default:
						break;
					}
				}
			}
			return list;
		}
		return Collections.emptyList();
	}

	private static class ServiceKey {
		String id;
		String service;
		
		public ServiceKey(String id, String service) {
			super();
			this.id = id;
			this.service = service;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result
					+ ((service == null) ? 0 : service.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ServiceKey other = (ServiceKey) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (service == null) {
				if (other.service != null)
					return false;
			} else if (!service.equals(other.service))
				return false;
			return true;
		}
	}

	/**
	 * Add parameter declaration to service
	 * @param serviceId
	 * @param decl
	 * @param ownerId
	 * @throws ResourceException 
	 */
	public Service addResourceDeclaration(String serviceId, ResourceDeclaration decl, String ownerId) throws ResourceException {
		validateResourceDeclarationData(decl);
		ServiceDescriptor sd = serviceRepository.findOne(serviceId);
		if (sd == null) throw new EntityNotFoundException("Not found: "+ serviceId);
		Service s = Utils.toServiceObject(sd);
		boolean found = false;
		for (ResourceDeclaration rd : s.getResource()) {
			if (rd.getId().equals(decl.getId())) {
				rd.setDescription(decl.getDescription());
				rd.setName(decl.getName());
				found  = true;
				break;
			}
		}
		if (!found) {
			s.getResource().add(decl);
		}
		updateService(s, ownerId);
		return s;
	}

	/**
	 * @param decl
	 */
	private void validateResourceDeclarationData(ResourceDeclaration decl) {
		if (!StringUtils.hasText(decl.getId())) {
			throw new IllegalArgumentException("empty resource parameter ID");
		}
		if (!StringUtils.hasText(decl.getName())) {
			throw new IllegalArgumentException("empty resource parameter name");
		}
		if (!StringUtils.hasText(decl.getDescription())) {
			throw new IllegalArgumentException("empty resource parameter description");
		}
	}

	/**
	 * @param mapping
	 */
	private void validateResourceMappingData(ResourceMapping mapping) {
		if (!StringUtils.hasText(mapping.getId())) {
			throw new IllegalArgumentException("empty resource mapping ID");
		}
		if (!StringUtils.hasText(mapping.getUri())) {
			throw new IllegalArgumentException("empty resource mapping uri");
		}
		if (!StringUtils.hasText(mapping.getName())) {
			throw new IllegalArgumentException("empty resource mapping name");
		}
		if (!StringUtils.hasText(mapping.getDescription())) {
			throw new IllegalArgumentException("empty resource mapping description");
		}
	}

	/**
	 * Add parameter declaration to service
	 * @param serviceId
	 * @param mapping
	 * @param ownerId
	 * @throws ResourceException 
	 */
	public Service addMapping(String serviceId, ResourceMapping mapping, String ownerId) throws ResourceException {
		validateResourceMappingData(mapping);
		ServiceDescriptor sd = serviceRepository.findOne(serviceId);
		if (sd == null) throw new EntityNotFoundException("Not found: "+ serviceId);
		Service s = Utils.toServiceObject(sd);
		boolean found = false;
		for (ResourceMapping rm : s.getResourceMapping()) {
			if (rm.getId().equals(mapping.getId())) {
				rm.setAccessibleByOthers(mapping.isAccessibleByOthers());
				rm.setApprovalRequired(mapping.isAccessibleByOthers());
				rm.setAuthority(mapping.getAuthority());
				rm.setDescription(mapping.getDescription());
				rm.setName(mapping.getName());
				rm.setUri(mapping.getUri());
				found  = true;
				break;
			}
		}
		if (!found) {
			s.getResourceMapping().add(mapping);
		}
		updateService(s, ownerId);
		return s;
	}

	/**
	 * @param serviceId
	 * @param ownerId
	 */
	public void checkServiceOwnership(String serviceId, String ownerId) {
		ServiceDescriptor sd = serviceRepository.findOne(serviceId);
		if (sd == null || !sd.getOwnerId().equals(ownerId)) {
			throw new IllegalArgumentException("Incorrect owner for service");
		}
	}

	/**
	 * Remove specified resource declaration
	 * @param serviceId
	 * @param id
	 * @param ownerId
	 * @return
	 * @throws ResourceException 
	 */
	public Object removeResourceDeclaration(String serviceId, String id, String ownerId) throws ResourceException {
		ServiceDescriptor sd = serviceRepository.findOne(serviceId);
		if (sd == null) throw new EntityNotFoundException("Not found: "+ serviceId);
		Service s = Utils.toServiceObject(sd);
		for (Iterator<ResourceDeclaration> iterator = s.getResource().iterator(); iterator.hasNext();) {
			ResourceDeclaration rd = iterator.next();
			if (rd.getId().equals(id)) {
				iterator.remove();
			}
		}
		updateService(s, ownerId);
		return s;
	}

	/**
	 * Remove specified resource mapping
	 * @param serviceId
	 * @param id
	 * @param ownerId
	 * @return
	 * @throws ResourceException 
	 */
	public Object removeMapping(String serviceId, String id, String ownerId) throws ResourceException {
		ServiceDescriptor sd = serviceRepository.findOne(serviceId);
		if (sd == null) throw new EntityNotFoundException("Not found: "+ serviceId);
		Service s = Utils.toServiceObject(sd);
		for (Iterator<ResourceMapping> iterator = s.getResourceMapping().iterator(); iterator.hasNext();) {
			ResourceMapping rm = iterator.next();
			if (rm.getId().equals(id)) {
				iterator.remove();
			}
		}
		updateService(s, ownerId);
		return s;
	}

}
