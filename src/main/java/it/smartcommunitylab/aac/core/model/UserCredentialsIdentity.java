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

package it.smartcommunitylab.aac.core.model;

import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import java.util.Collection;
import org.springframework.security.core.CredentialsContainer;

public interface UserCredentialsIdentity extends UserIdentity, CredentialsContainer {
    // credentials
    public Collection<UserCredentials> getCredentials();
}
