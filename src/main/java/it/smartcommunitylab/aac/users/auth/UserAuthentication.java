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
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.users.model.UserDetails;
import java.time.Instant;
import java.util.Set;
import org.springframework.security.core.Authentication;

/**
 *
 */
public interface UserAuthentication extends Authentication {
    public Subject getSubject();

    public String getRealm();

    public String getSubjectId();

    @JsonIgnore
    public UserDetails getUser();

    public Instant getCreatedAt();

    public long getAge();

    /*
     * Auth tokens
     */

    public ExtendedAuthenticationToken getAuthentication(String authority, String provider, String userId);

    public void eraseAuthentication(ExtendedAuthenticationToken auth);

    public Set<ExtendedAuthenticationToken> getAuthentications();

    public boolean isExpired();

    /*
     * web auth details
     */
    public WebAuthenticationDetails getWebAuthenticationDetails();
}
