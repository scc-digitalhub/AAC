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

package it.smartcommunitylab.aac.oauth.client;

import org.springframework.stereotype.Service;

/*
 * Manager for OAuth2 Clients
 *
 * should serve APIs or custom services, client persistence is handled by clientService
 */

@Service
public class OAuth2ClientManager {
    // TODO clientRegistration as per OAuth2 spec
    //    public OAuth2ClientRegistration register();
}
