package it.smartcommunitylab.aac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.manager.ProviderServiceAdapter;
import it.smartcommunitylab.aac.manager.ResourceManager;
import it.smartcommunitylab.aac.manager.RoleManager;

@Component
public class AACInitializer implements ApplicationListener<ApplicationReadyEvent> {
 
	
	@Autowired
	private RoleManager roleManager;
	@Autowired
	private ResourceManager resourceManager;
	@Autowired
	private ProviderServiceAdapter providerServiceAdapter;

	private static final Logger logger = LoggerFactory.getLogger(AACInitializer.class);
	
	public void onApplicationEvent(ApplicationReadyEvent event) {
		try {
			resourceManager.init();
			providerServiceAdapter.init();
			roleManager.init();
		} catch (Exception e) {
			logger .error(e.getMessage(), e);
		}
	}
	
}
