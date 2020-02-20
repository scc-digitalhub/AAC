package it.smartcommunitylab.aac.apim;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import it.smartcommunitylab.aac.keymanager.model.AACService;
import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

@Component
//@Transactional
public class APIMProviderService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	@Autowired
	private ClientDetailsManager clientDetailsManager;	
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ServiceManager serviceManager;
	@Value("${api.contextSpace}")
	private String apiProviderContext;

	public ClientAppBasic createClient(ClientAppBasic app, String userName) throws Exception {
		User user = userRepository.findByUsername(userName);
		
		if (user == null) {
			return null;
		}
		
		ClientAppBasic resApp = clientDetailsManager.createOrUpdate(app, user.getId());
		resApp.setRedirectUris(app.getRedirectUris());
		resApp.setGrantedTypes(app.getGrantedTypes());
		clientDetailsManager.update(resApp.getClientId(), resApp);
		
		return resApp;
	}	
	
	public ClientAppBasic updateClient(String clientId, ClientAppBasic app) throws Exception {
		ClientAppBasic resApp = clientDetailsManager.update(clientId, app);

		return resApp;
	}
	
	public void updateValidity(String clientId, Integer validity) throws Exception {
		ClientDetailsEntity entity = clientDetailsRepository.findByClientId(clientId);
		
		entity.setAccessTokenValidity(validity);
//		entity.setRefreshTokenValidity(validity);

		clientDetailsRepository.save(entity);
	}	
	
	public void updateClientScope(String clientId, String scope) throws Exception {
		ClientDetailsEntity entity = clientDetailsRepository.findByClientId(clientId);
		Set<String> oldScope = entity.getScope();
		oldScope.addAll(Splitter.on(",").splitToList(scope));
		entity.setScope(Joiner.on(",").join(oldScope));
		
		Set<String> serviceIds = serviceManager.findServiceIdsByScopes(entity.getScope());
		entity.setResourceIds(StringUtils.collectionToCommaDelimitedString(serviceIds));		
		
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
	
	public boolean createResource(AACService service, String userName, String tenant) throws Exception {
		// Do nothing: scopes / services should already exist
		return true;
	}
	
	public void deleteResource(String resourceName) throws Exception {
		// Do nothing: scopes / services should already exist
	}
		
}
