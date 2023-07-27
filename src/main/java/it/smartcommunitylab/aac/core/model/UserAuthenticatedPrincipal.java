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

import java.io.Serializable;
import java.util.Map;
import org.springframework.security.core.AuthenticatedPrincipal;

/*
 * An authenticated user principal, associated with an identity provider and an account as a user resource
 *
 * Every identity provider should implement a custom class exposing this interface for handling the authentication flow
 */
public interface UserAuthenticatedPrincipal extends AuthenticatedPrincipal, UserResource, Serializable {
    // principal name
    public String getUsername();

    // principal email
    public String getEmailAddress();

    public boolean isEmailVerified();

    // principal attributes as received from idp
    public Map<String, Serializable> getAttributes();

    // principalId is local to the provider
    String getPrincipalId();
}
