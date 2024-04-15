/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.oauth.flow;

import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FlowExtensionsService implements InitializingBean {

    @Autowired
    private ScriptExecutionService executionService;

    private ScriptOAuthFlowExtensions scriptFlowExtensions;
    private WebhookOAuthFlowExtensions webhookFlowExtensions;

    public OAuthFlowExtensions getOAuthFlowExtensions(OAuth2ClientDetails clientDetails) {
        // always use delegateFlowExtensions to support multiple extensions
        List<OAuthFlowExtensions> extensions = new ArrayList<>();

        // first hook is script
        if (
            scriptFlowExtensions != null &&
            clientDetails.getHookFunctions() != null &&
            !clientDetails.getHookFunctions().isEmpty()
        ) {
            extensions.add(scriptFlowExtensions);
        }

        // then process webHook
        if (clientDetails.getHookWebUrls() != null && !clientDetails.getHookWebUrls().isEmpty()) {
            extensions.add(webhookFlowExtensions);
        }

        if (!extensions.isEmpty()) {
            return new DelegateOAuthFlowExtensions(extensions);
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
