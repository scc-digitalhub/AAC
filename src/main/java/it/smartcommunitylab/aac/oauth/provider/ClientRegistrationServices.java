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

package it.smartcommunitylab.aac.oauth.provider;

import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import it.smartcommunitylab.aac.oauth.request.ClientRegistrationRequest;
import org.springframework.security.oauth2.provider.ClientRegistrationException;

public interface ClientRegistrationServices {
    public ClientRegistration loadRegistrationByClientId(String clientId) throws ClientRegistrationException;

    public ClientRegistration addRegistration(String realm, ClientRegistrationRequest request)
        throws ClientRegistrationException;

    public ClientRegistration updateRegistration(String clientId, ClientRegistrationRequest request)
        throws ClientRegistrationException;

    public void removeRegistration(String clientId);
}
