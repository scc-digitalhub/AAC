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

package it.smartcommunitylab.aac.oauth.approval;

import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import org.springframework.security.oauth2.provider.AuthorizationRequest;

public interface ClientApprovalHandler {
    /*
     * evaluate if resourceIds are approved for the given client
     */
    boolean isApproved(AuthorizationRequest authorizationRequest, OAuth2ClientDetails clientDetails);

    /*
     * Hook for allowing modifications to the request to modify based on resourceIds
     * approval
     */
    AuthorizationRequest checkForPreApproval(
        AuthorizationRequest authorizationRequest,
        OAuth2ClientDetails clientDetails
    );
}
