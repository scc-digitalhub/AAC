package it.smartcommunitylab.aac.oauth.flow;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;

@Service
public class FlowExtensionsService implements InitializingBean {

    @Autowired
    private ScriptExecutionService executionService;

    private ScriptOAuthFlowExtensions scriptFlowExtensions;
    private WebhookOAuthFlowExtensions webhookFlowExtensions;

    public OAuthFlowExtensions getOAuthFlowExtensions(OAuth2ClientDetails clientDetails) {

        // TODO write delegateFlowExtensions to support multiple extensions
        if (scriptFlowExtensions != null && clientDetails.getHookFunctions() != null
                && !clientDetails.getHookFunctions().isEmpty()) {
            return scriptFlowExtensions;
        }

        if (clientDetails.getHookWebUrls() != null && !clientDetails.getHookWebUrls().isEmpty()) {
            return webhookFlowExtensions;
        }

        return null;

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        webhookFlowExtensions = new WebhookOAuthFlowExtensions();
        if (executionService != null) {
            scriptFlowExtensions = new ScriptOAuthFlowExtensions();
            scriptFlowExtensions.setExecutionService(executionService);
        }

    }

}
