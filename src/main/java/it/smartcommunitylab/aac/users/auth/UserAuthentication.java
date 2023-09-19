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

package it.smartcommunitylab.aac.users.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthentication;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.users.model.User;
import it.smartcommunitylab.aac.users.model.UserDetails;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import org.springframework.security.core.GrantedAuthority;

/**
 * A user authentication as used in the auth/security context
 */
public interface UserAuthentication extends ExtendedAuthentication {
    /*
     * User
     */

    @Override
    public default String getType() {
        return SystemKeys.RESOURCE_USER;
    }

    // user details reports user info relevant for the auth/security context
    // do note that user details are immutable within a session:
    // in order to refresh we need to build a new session
    public UserDetails getUserDetails();

    public default String getUserId() {
        return getUserDetails() != null ? getUserDetails().getUserId() : null;
    }

    @Override
    default Collection<? extends GrantedAuthority> getAuthorities() {
        return getUserDetails() != null ? getUserDetails().getAuthorities() : null;
    }

    // user (temporarily) stores user info and resources
    // for consumption *outside* the auth/security context
    // do note that this field *IS* refreshable, as in actual implementations
    // could update the field as needed during the session lifetime
    public @Nullable User getUser();

    public void setUser(User user);

    /*
     * web auth details
     * TODO remove from here, some auth could be NOT web
     */
    public @Nullable WebAuthenticationDetails getWebAuthenticationDetails();

    @Override
    default Object getDetails() {
        //web auth are the default details,
        return getWebAuthenticationDetails();
    }
}
