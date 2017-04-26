package it.smartcommunitylab.aac;

import it.smartcommunitylab.aac.apimanager.APIProviderManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.model.User;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AACInitializer {

	@Autowired
	APIProviderManager apiProviderManager;
	
	@Autowired
	RoleManager roleManager;
	
	@PostConstruct
	public void init() throws Exception {
		User admin = roleManager.init();
		apiProviderManager.init(admin.getId());
	}
	
}
