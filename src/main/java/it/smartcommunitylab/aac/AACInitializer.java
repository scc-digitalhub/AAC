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

    private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private RoleManager roleManager;
	@Autowired
	private ResourceManager resourceManager;
	@Autowired
	private ProviderServiceAdapter providerServiceAdapter;

	
	public void onApplicationEvent(ApplicationReadyEvent event) {
	    logger.debug("initialize components on application ready");
		try {
			resourceManager.init();
			providerServiceAdapter.init();
			roleManager.init();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
}
