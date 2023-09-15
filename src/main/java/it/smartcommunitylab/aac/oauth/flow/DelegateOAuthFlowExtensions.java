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

import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.users.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.Assert;

public class DelegateOAuthFlowExtensions implements OAuthFlowExtensions {

    private List<OAuthFlowExtensions> flowExtensions;

    public DelegateOAuthFlowExtensions(OAuthFlowExtensions... flowExtensions) {
        this(Arrays.asList(flowExtensions));
    }

    public DelegateOAuthFlowExtensions(List<OAuthFlowExtensions> flowExtensions) {
        Assert.notNull(flowExtensions, "extensions can not be null");
        setFlowExtensions(flowExtensions);
    }

    public void setFlowExtensions(List<OAuthFlowExtensions> flowExtensions) {
        this.flowExtensions = new ArrayList<>(flowExtensions);
    }

    @Override
    public Map<String, String> onBeforeUserApproval(
        Map<String, String> requestParameters,
        User user,
        OAuth2ClientDetails client
    ) throws FlowExecutionException {
        // iterate and let all extensions process hook
        Map<String, String> parameters = new HashMap<>();
        parameters.putAll(requestParameters);

        for (OAuthFlowExtensions fe : flowExtensions) {
            parameters = fe.onBeforeUserApproval(parameters, user, client);
        }

        return parameters;
    }

    @Override
    public Optional<Boolean> onAfterUserApproval(Collection<String> scopes, User user, OAuth2ClientDetails client)
        throws FlowExecutionException {
        // iterate and let all extensions process hook
        // null by default, we don't want to modify the decision
        Boolean result = null;

        for (OAuthFlowExtensions fe : flowExtensions) {
            Optional<Boolean> r = fe.onAfterUserApproval(scopes, user, client);
            if (r.isPresent()) {
                result = r.get();
            }
        }

        return Optional.ofNullable(result);
    }

    @Override
    public Map<String, String> onBeforeTokenGrant(Map<String, String> requestParameters, OAuth2ClientDetails client)
        throws FlowExecutionException {
        // iterate and let all extensions process hook
        Map<String, String> parameters = new HashMap<>();
        parameters.putAll(requestParameters);

        for (OAuthFlowExtensions fe : flowExtensions) {
            parameters = fe.onBeforeTokenGrant(parameters, client);
        }

        return parameters;
    }

    @Override
    public void onAfterTokenGrant(OAuth2AccessToken accessToken, OAuth2ClientDetails client)
        throws FlowExecutionException {
        // iterate and let all extensions process hook

        for (OAuthFlowExtensions fe : flowExtensions) {
            fe.onAfterTokenGrant(accessToken, client);
        }
    }
}
