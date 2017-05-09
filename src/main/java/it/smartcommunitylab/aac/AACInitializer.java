package it.smartcommunitylab.aac;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.apimanager.APIProviderManager;
import it.smartcommunitylab.aac.manager.ProviderServiceAdapter;
import it.smartcommunitylab.aac.manager.ResourceManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.model.User;

@Component
public class AACInitializer {

	@Autowired
	private APIProviderManager apiProviderManager;
	
	@Autowired
	private RoleManager roleManager;
	@Autowired
	private ResourceManager resourceManager;
	@Autowired
	private ProviderServiceAdapter providerServiceAdapter;
	
	@PostConstruct
	public void init() throws Exception {
		resourceManager.init();
		providerServiceAdapter.init();
		User admin = roleManager.init();
		apiProviderManager.init(admin.getId());
	}
	
}
