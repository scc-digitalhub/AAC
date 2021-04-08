package it.smartcommunitylab.aac.services;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.claims.ExtractorsRegistry;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

@Component
public class ServicesManager implements InitializingBean {

    @Autowired
    private ServicesService serviceService;

    @Autowired
    private ScopeRegistry scopeRegistry;

    @Autowired
    private ExtractorsRegistry extractorsRegistry;

    private ScriptServiceClaimExtractor scriptClaimExtractor;

    public ServicesManager() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        scriptClaimExtractor = new ScriptServiceClaimExtractor(serviceService);
        extractorsRegistry.registerExtractor(scriptClaimExtractor);
    }

    /*
     * Services
     */

    /*
     * Scopes
     */

    /*
     * Claims
     */
}
