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

package it.smartcommunitylab.aac.auth.model;

import it.smartcommunitylab.aac.model.Subject;
import java.time.Instant;
import java.util.Set;
import javax.annotation.Nullable;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;

/**
 * An extended authentication as used in the auth/security context
 */
public interface ExtendedAuthentication extends Authentication, CredentialsContainer {
    /*
     * Principal
     */
    //realm *of the authentication*, could be different from user/subject realm
    public String getRealm();

    // subject identifies the entity performing auth
    // do note that subject is immutable within a session:
    // in order to refresh we need to build a new session
    public Subject getSubject();

    public default String getSubjectId() {
        return getSubject() != null ? getSubject().getId() : null;
    }

    @Override
    public default String getName() {
        return getSubject() != null ? getSubject().getName() : null;
    }

    public default String getType() {
        return getSubject() != null ? getSubject().getType() : null;
    }

    /*
     * Session information
     */

    //TODO define: logical sessionId should be separated from internal session
    // public String getSessionId();

    public Instant getCreatedAt();

    public long getAge();

    public boolean isExpired();

    /*
     * Auth tokens
     */

    public Set<ExtendedAuthenticationToken<? extends AuthenticatedPrincipal>> getAuthentications();

    @Override
    default void eraseCredentials() {
        if (getAuthentications() != null) {
            //clear sensitive information from tokens, delegate to implementations
            getAuthentications().forEach(c -> c.eraseCredentials());
        }
    }

    @Override
    public default Object getCredentials() {
        //auth tokens are the credentials
        return getAuthentications();
    }

    /*
     * Auth details
     * TODO add info about authentication class, security level, MFA etc
     */

    @Override
    public default Object getPrincipal() {
        //subject is the default principal
        //we expect implementations to provide *Details about the subject as principal
        return getSubject();
    }

    public @Nullable WebAuthenticationDetails getWebAuthenticationDetails();

    @Override
    default Object getDetails() {
        //web auth are the default details,
        return getWebAuthenticationDetails();
    }
}
