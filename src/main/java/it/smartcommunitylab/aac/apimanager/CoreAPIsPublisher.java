package it.smartcommunitylab.aac.apimanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CoreAPIsPublisher {

	@Autowired
	private APIManager apiManager;
	@Autowired
	private APIProviderManager providerManager;

	@Value("${admin.password}")
	private String adminPassword;

	public void init() throws Exception {
		String token = providerManager.createToken("admin", adminPassword);

		apiManager.publishAPI("api/profile-api.json", "AAC", "AAC User Profile APIs", "/aacprofile", token);
		apiManager.publishAPI("api/roles-api.json", "AACRoles", "AAC User Roles APIs", "/aacroles", token);
		apiManager.publishAPI("api/authorization-api.json", "AACAuthorization", "AAC Authorization APIs", "/aacauthorization", token);
		apiManager.publishAPI("api/key-api.json", "AACKeys", "AAC API Key APIs", "/aacapikey", token);
	}

}
