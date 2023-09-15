/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.oauth.flow;

import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.users.model.User;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * OAuth 2.0 Flow extensions manager. Handles specific Flow phases, in
 * particular AfterApproval - immediately after the user has approved the
 * requested scopes and before the code/token is emitted.
 *
 * @author raman
 *
 */
public interface OAuthFlowExtensions {
    public static final String BEFORE_USER_APPROVAL = "beforeUserApproval";
    public static final String AFTER_USER_APPROVAL = "afterUserApproval";

    public static final String BEFORE_TOKEN_GRANT = "beforeTokenGrant";
    public static final String AFTER_TOKEN_GRANT = "afterTokenGrant";

    /**
     * Event triggered immediately before the user approves the requested scopes and
     * before the code/token is emitted.
     *
     * The returned parameters can be modified to alter the process
     */
    public Map<String, String> onBeforeUserApproval(
        Map<String, String> requestParameters,
        User user,
        OAuth2ClientDetails client
    ) throws FlowExecutionException;

    /**
     * Event triggered immediately after the user has approved the requested scopes
     * and before the code/token is emitted.
     *
     * The returned request can have the authorized status changed from true to
     * false, any other modifications will be discarded
     */
    public Optional<Boolean> onAfterUserApproval(Collection<String> scopes, User user, OAuth2ClientDetails client)
        throws FlowExecutionException;

    /**
     * Event triggered immediately before the token generation
     *
     * The returned parameters can be modified to alter the process
     */
    public Map<String, String> onBeforeTokenGrant(Map<String, String> requestParameters, OAuth2ClientDetails client)
        throws FlowExecutionException;

    /*
     * Event triggered immediately after the token generation
     */

    public void onAfterTokenGrant(OAuth2AccessToken accessToken, OAuth2ClientDetails client)
        throws FlowExecutionException;
}
