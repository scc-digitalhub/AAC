package it.smartcommunitylab.aac;

import it.smartcommunitylab.aac.apimanager.APIProviderManager;
import it.smartcommunitylab.aac.manager.ResourceManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.model.User;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AACInitializer {

	@Autowired
	private APIProviderManager apiProviderManager;
	
	@Autowired
	private RoleManager roleManager;
	@Autowired
	private ResourceManager resourceManager;
	
	@PostConstruct
	public void init() throws Exception {
		resourceManager.init();
		User admin = roleManager.init();
		apiProviderManager.init(admin.getId());
	}
	
}
