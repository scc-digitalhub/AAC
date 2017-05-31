package it.smartcommunitylab.aac.apimanager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import it.smartcommunitylab.aac.wso2.model.API;
import it.smartcommunitylab.aac.wso2.services.APIPublisherService;

@Component
public class CoreAPIsPublisher {

	@Autowired
	private APIPublisherService pub;
	@Autowired
	private APIProviderManager providerManager;
	
	 @Autowired
	 private ConfigurableEnvironment env;

	@Value("${admin.password}")
	private String adminPassword;

	private final static String ENDPOINT_CONFIG = "{\"production_endpoints\":{\"url\":\"${application.internalUrl}\",\"config\":null},\"sandbox_endpoints\":{\"url\":\"${application.internalUrl}\",\"config\":null},\"endpoint_type\":\"http\"}";

	public void init() throws Exception {
		String token = providerManager.createToken("admin", adminPassword);
		
		publishAPI("api/profile-api.json", "AAC", "AAC User Profile APIs", "/aacprofile", token);
		publishAPI("api/roles-api.json", "AACRoles", "AAC User Roles APIs", "/aacroles", token);
		publishAPI("api/authorization-api.json", "AACAuthorization", "AAC Authorization APIs", "/aacauthorization", token);
	}
	
	@SuppressWarnings("rawtypes")
	private void publishAPI(String json, String name, String description, String context, String token) throws IOException{
		Map apis = pub.findAPI(name, token);

		List list = (List) apis.get("list");
//		System.err.println(apis);
		if (list.isEmpty() || !list.stream().anyMatch(x -> name.equals(((Map)x).get("name")))) {
			ObjectMapper mapper = new ObjectMapper();

			String swagger = Resources.toString(Resources.getResource(json), Charsets.UTF_8);
			swagger = env.resolvePlaceholders(swagger);			
			
			API api = new API();

			api.setName(name);
			api.setDescription(description);
			api.setContext(context);
			api.setVersion("1.0.0");
			api.setProvider("admin");
			api.setApiDefinition(swagger);
			api.setStatus("CREATED");
			api.setVisibility("PUBLIC");

			api.setIsDefaultVersion(true);
			api.setEndpointConfig(env.resolvePlaceholders(ENDPOINT_CONFIG));			
			
			API result = pub.publishAPI(api, token);
			pub.changeAPIStatus(result.getId(), "Publish", token);
//			System.err.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
		}
	}

}
