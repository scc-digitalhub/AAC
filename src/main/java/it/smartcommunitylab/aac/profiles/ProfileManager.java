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

package it.smartcommunitylab.aac.profiles;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.EmailProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;
import it.smartcommunitylab.aac.profiles.service.ProfileService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class ProfileManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProfileService profileService;

    @Autowired
    private AuthenticationHelper authHelper;

    /*
     * Current user, from context - shortcuts
     */

    public BasicProfile curBasicProfile() throws InvalidDefinitionException {
        return (BasicProfile) profileService.getProfile(curUserDetails(), BasicProfile.IDENTIFIER);
    }

    public OpenIdProfile curOpenIdProfile() throws InvalidDefinitionException {
        return (OpenIdProfile) profileService.getProfile(curUserDetails(), OpenIdProfile.IDENTIFIER);
    }

    public EmailProfile curEmailProfile() throws InvalidDefinitionException {
        return (EmailProfile) profileService.getProfile(curUserDetails(), EmailProfile.IDENTIFIER);
    }

    public Collection<AccountProfile> curAccountProfiles() throws InvalidDefinitionException {
        Collection<AccountProfile> profiles = profileService
            .getProfiles(curUserDetails(), AccountProfile.IDENTIFIER)
            .stream()
            .map(a -> (AccountProfile) a)
            .collect(Collectors.toList());

        return profiles;
    }

    /*
     * Users from db
     */

    public AbstractProfile getProfile(String realm, String subjectId, String identifier)
        throws NoSuchUserException, InvalidDefinitionException {
        return profileService.getProfile(realm, subjectId, identifier);
    }

    public Collection<AbstractProfile> getProfiles(String realm, String subjectId, String identifier)
        throws NoSuchUserException, InvalidDefinitionException {
        Collection<AbstractProfile> profiles = new ArrayList<>();
        profiles.addAll(profileService.getProfiles(realm, subjectId, identifier));
        return profiles;
    }

    /*
     * Helpers
     */

    private UserDetails curUserDetails() {
        UserDetails details = authHelper.getUserDetails();
        if (details == null) {
            throw new InsufficientAuthenticationException("invalid or missing user authentication");
        }

        return details;
    }
}
