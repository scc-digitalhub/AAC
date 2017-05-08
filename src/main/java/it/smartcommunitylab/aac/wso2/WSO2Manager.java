package it.smartcommunitylab.aac.wso2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.RESOURCE_VISIBILITY;
import it.smartcommunitylab.aac.apimanager.APIProviderManager;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.jaxbmodel.Authority;
import it.smartcommunitylab.aac.jaxbmodel.ResourceMapping;
import it.smartcommunitylab.aac.keymanager.model.AACResource;
import it.smartcommunitylab.aac.keymanager.model.AACService;
import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.ResourceManager;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.Resource;
import it.smartcommunitylab.aac.model.ServiceDescriptor;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.RegistrationRepository;
import it.smartcommunitylab.aac.repository.ResourceRepository;
import it.smartcommunitylab.aac.repository.ServiceRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

@Component
//@Transactional
public class WSO2Manager {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	@Autowired
	private ResourceRepository resourceRepository;
	@Autowired
	private APIProviderManager providerManager;
	@Autowired
	private ClientDetailsManager clientDetailsManager;	
	@Autowired
	private RegistrationRepository registrationRepository;
	@Autowired
	private ResourceManager resourceManager;	
	@Autowired
	private ServiceRepository serviceRepository;
	
	public ClientAppBasic createClient(ClientAppBasic app, String userName) throws Exception {
		String un = Utils.extractUserFromTenant(userName);
		
		Registration registration = registrationRepository.findByEmail(un);
		
		if (registration == null) {
			return null;
		}
		
		ClientAppBasic resApp = clientDetailsManager.createOrUpdate(app, Long.parseLong(registration.getUserId()));
		resApp.setRedirectUris(app.getRedirectUris());
		resApp.setGrantedTypes(app.getGrantedTypes());
		clientDetailsManager.update(resApp.getClientId(), resApp);
		
		// TODO: remove!
//		clientDetailsManager.delete(resApp.getClientId());
		
		return resApp;
	}	
	
	public ClientAppBasic updateClient(String clientId, ClientAppBasic app) throws Exception {
		ClientAppBasic resApp = clientDetailsManager.update(clientId, app);

		return resApp;
	}
	
	public void updateClientScope(String consumerKey, String scope) throws Exception {
		ClientDetailsEntity entity = clientDetailsRepository.findByClientId(consumerKey);
		Set<String> oldScope = entity.getScope();
		oldScope.addAll(Splitter.on(",").splitToList(scope));
		entity.setScope(Joiner.on(",").join(oldScope));
		
		String resourcesId = "";
		for (String resource : entity.getScope()) {
			it.smartcommunitylab.aac.model.Resource r = resourceRepository.findByResourceUri(resource);
			if (r != null) {
				resourcesId += "," + r.getResourceId();
			}
		}
		resourcesId =resourcesId.replaceFirst(",", "");
		entity.setResourceIds(resourcesId);		
		
		ClientAppBasic resApp = clientDetailsManager.convertToClientApp(entity);
		clientDetailsManager.update(entity.getClientId(), resApp);
	}		
	
	public ClientAppBasic getClient(String consumerKey) throws Exception {
		ClientDetailsEntity entity = clientDetailsRepository.findByClientId(consumerKey);
		ClientAppBasic resApp = clientDetailsManager.convertToClientApp(entity);
		
		return resApp;
	}	

	public void deleteClient(String clientId) throws Exception {
		ClientDetailsEntity entity = clientDetailsRepository.findByClientId(clientId);
		clientDetailsRepository.delete(entity);
	}	
	
	public boolean createResource(AACService service, String userName) throws Exception {
		
		Registration registration = registrationRepository.findByEmail(userName);
		
		if (registration == null) {
			return false;
		}		
		
		String serviceId = service.getServiceName();
		
		ServiceDescriptor sd = new ServiceDescriptor();
		sd.setServiceId(serviceId);
		sd.setServiceName(serviceId);
		sd.setDescription(service.getDescription());
		sd.setOwnerId(registration.getUserId());		
		sd.setResourceDefinitions(new ArrayList<String>().toString());
		sd.setResourceMappings(resourcesToJSON(service.getResources()));
		
		sd = serviceRepository.save(sd);
		
		List<Resource> resources = resourceRepository.findByService(sd);
		Map<String, Resource> oldUris = resources.stream().collect(Collectors.toMap(x -> x.getResourceUri(), x -> x));
		
		for (AACResource aacResource: service.getResources()) {
			String id = aacResource.getResourceUri();

			Resource old = resourceRepository.findByResourceUri(id);
			
			Resource resource;
			
			if (old != null) {
				resource = old;
			} else {
				resource = new Resource();
			}
			
			resource.setName(aacResource.getName());
			resource.setDescription(aacResource.getDescription());
			resource.setResourceType(id);
			resource.setResourceUri(id);
			resource.setAuthority(Config.AUTHORITY.ROLE_ANY);
			resource.setVisibility(RESOURCE_VISIBILITY.PUBLIC);
			resource.setRoles(Joiner.on(",").join(aacResource.getRoles()));
			
			resource.setService(sd);
			
			resourceRepository.save(resource);
			
			oldUris.remove(id);
			
		}
		
		oldUris.values().stream().forEach(x -> resourceRepository.delete(x));
		
		return true;
	}
	
	public void deleteResource(String resourceName) throws Exception {
		if (serviceRepository.findOne(resourceName) != null) {
			serviceRepository.delete(resourceName);
		}
	}
	
	
	private String resourcesToJSON(List<AACResource> resources) throws Exception {
		List<ResourceMapping> list = Lists.newArrayList();
		for (AACResource resource: resources) {
			Map<String, Object> map = Maps.newTreeMap();
			ResourceMapping rm = new ResourceMapping();
			
			rm.setId(resource.getResourceUri());
			rm.setName(resource.getName());
			rm.setDescription(resource.getDescription());
			rm.setAuthority(Authority.ROLE_ANY); // TODO
			rm.setApprovalRequired(false); // TODO
			rm.setAccessibleByOthers(false); // TODO
			rm.setUri(resource.getResourceUri());
			list.add(rm);
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(list);
	}
	
}
