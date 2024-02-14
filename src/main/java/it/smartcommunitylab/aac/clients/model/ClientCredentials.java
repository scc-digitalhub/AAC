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

package it.smartcommunitylab.aac.clients.model;

import it.smartcommunitylab.aac.model.Credentials;
import org.springframework.security.core.CredentialsContainer;

public interface ClientCredentials extends Credentials, CredentialsContainer {
    public String getClientId();

    // by default client credentials are active
    // TODO handle at implementation level
    public default boolean isActive() {
        return true;
    }

    public default boolean isExpired() {
        return false;
    }

    public default boolean isRevoked() {
        return false;
    }
}
