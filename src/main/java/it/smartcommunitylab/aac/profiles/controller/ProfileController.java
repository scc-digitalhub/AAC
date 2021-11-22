/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package it.smartcommunitylab.aac.profiles.controller;

import java.io.IOException;
import java.util.Collection;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.profiles.ProfileManager;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;
import it.smartcommunitylab.aac.profiles.model.ProfileResponse;

/**
 * @author raman
 *
 */
@RestController
@Tag(name= "AAC User profile" )
public class ProfileController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProfileManager profileManager;

    /*
     * Current user operations TODO evaluate move/copy to MVC controller and keep
     * this API only, with bearer token
     */

    @Deprecated()
    @Operation(summary= "Get basic profile of the current user")
    @PreAuthorize("hasAuthority('" + Config.R_USER + "') and hasAuthority('SCOPE_" + Config.SCOPE_BASIC_PROFILE + "')")
    @GetMapping(value = "/basicprofile/me")
    public @ResponseBody ProfileResponse myBasicProfile(BearerTokenAuthentication auth)
            throws InvalidDefinitionException, NoSuchUserException {
        if (auth == null) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        String subject = (String) auth.getTokenAttributes().get("sub");
        String realm = (String) auth.getTokenAttributes().get("realm");

        if (!StringUtils.hasText(subject) || !StringUtils.hasText(realm)) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        AbstractProfile profile = profileManager.getProfile(realm, subject, BasicProfile.IDENTIFIER);
        return new ProfileResponse(subject, profile);
    }

    @Deprecated()
    @Operation(summary= "Get openid profile of the current user")
    @PreAuthorize("hasAuthority('" + Config.R_USER + "') and hasAuthority('SCOPE_" + Config.SCOPE_PROFILE + "')")
    @GetMapping(value = "/openidprofile/me")
    public @ResponseBody ProfileResponse myOpenIdProfile(BearerTokenAuthentication auth)
            throws InvalidDefinitionException, NoSuchUserException {
        if (auth == null) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        String subject = (String) auth.getTokenAttributes().get("sub");
        String realm = (String) auth.getTokenAttributes().get("realm");

        if (!StringUtils.hasText(subject) || !StringUtils.hasText(realm)) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        AbstractProfile profile = profileManager.getProfile(realm, subject, OpenIdProfile.IDENTIFIER);
        return new ProfileResponse(subject, profile);
    }

    @Deprecated()
    @Operation(summary= "Get account profiles of the current user")
    @PreAuthorize("hasAuthority('" + Config.R_USER + "') and hasAuthority('SCOPE_" + Config.SCOPE_ACCOUNT_PROFILE
            + "')")
    @GetMapping(value = "/accountprofile/me")
    public @ResponseBody ProfileResponse myAccountProfiles(BearerTokenAuthentication auth)
            throws InvalidDefinitionException, NoSuchUserException {
        if (auth == null) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        String subject = (String) auth.getTokenAttributes().get("sub");
        String realm = (String) auth.getTokenAttributes().get("realm");

        if (!StringUtils.hasText(subject) || !StringUtils.hasText(realm)) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        Collection<AbstractProfile> profiles = profileManager.getProfiles(realm, subject, AccountProfile.IDENTIFIER);
        return new ProfileResponse(subject, profiles);
    }

    @Operation(summary= "Get profiles of the current user")
    @PreAuthorize("hasAuthority('" + Config.R_USER + "') and hasAuthority('SCOPE_profile.'+#identifier+'.me')")
    @GetMapping(value = "/profile/{identifier}/me")
    public @ResponseBody ProfileResponse myProfiles(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identifier,
            BearerTokenAuthentication auth)
            throws InvalidDefinitionException, NoSuchUserException {
        if (auth == null) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        String subject = (String) auth.getTokenAttributes().get("sub");
        String realm = (String) auth.getTokenAttributes().get("realm");

        if (!StringUtils.hasText(subject) || !StringUtils.hasText(realm)) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        Collection<AbstractProfile> profiles = profileManager.getProfiles(realm, subject, identifier);
        return new ProfileResponse(subject, profiles);
    }

    /*
     * Debug
     */

    @RolesAllowed("ROLE_USER")
    @GetMapping(value = "/whoami")
    public @ResponseBody UserAuthentication debug(Authentication auth, HttpServletResponse response)
            throws IOException {

        // authentication should be a user authentication
        if (!(auth instanceof UserAuthentication)) {
            throw new InsufficientAuthenticationException("not a user authentication");
        }

        UserAuthentication token = (UserAuthentication) auth;

        return token;

    }

    @RolesAllowed({ "ROLE_USER", "ROLE_CLIENT" })
    @GetMapping(value = "/api/whoami")
    public @ResponseBody Authentication debugApi(Authentication auth, HttpServletResponse response)
            throws IOException {

        return auth;

    }

    /*
     * Api operations
     */
    @Operation(summary= "Get basic profile of a user")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + Config.SCOPE_BASIC_PROFILE_ALL
            + "')")
    @GetMapping(value = "/api/profiles/{realm}/basicprofile/{subject}")
    public @ResponseBody ProfileResponse getBasicProfile(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subject)
            throws NoSuchRealmException, NoSuchUserException, InvalidDefinitionException {
        AbstractProfile profile = profileManager.getProfile(realm, subject, BasicProfile.IDENTIFIER);
        return new ProfileResponse(subject, profile);
    }

    @Operation(summary= "Get openid profile of a user")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + Config.SCOPE_OPENID_PROFILE_ALL
            + "')")
    @GetMapping(value = "/api/profiles/{realm}/openidprofile/{subject}")
    public @ResponseBody ProfileResponse getOpenIdProfile(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subject)
            throws NoSuchRealmException, NoSuchUserException, InvalidDefinitionException {
        AbstractProfile profile = profileManager.getProfile(realm, subject, BasicProfile.IDENTIFIER);
        return new ProfileResponse(subject, profile);
    }

    @Operation(summary= "Get account profiles of a user")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + Config.SCOPE_ACCOUNT_PROFILE_ALL
            + "')")
    @GetMapping(value = "/api/profiles/{realm}/accountprofile/{subject}")
    public @ResponseBody ProfileResponse getAccountProfiles(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subject)
            throws NoSuchRealmException, NoSuchUserException, InvalidDefinitionException {
        Collection<AbstractProfile> profiles = profileManager.getProfiles(realm, subject, AccountProfile.IDENTIFIER);
        return new ProfileResponse(subject, profiles);
    }

    @Operation(summary= "Get custom profiles of a user")
//    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
//            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_profile.#identifier.all')")
    @GetMapping(value = "/api/profiles/{realm}/{identifier}/{subject}")
    public @ResponseBody ProfileResponse getProfiles(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String identifier,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String subject)
            throws NoSuchRealmException, NoSuchUserException, InvalidDefinitionException {
        Collection<AbstractProfile> profiles = profileManager.getProfiles(realm, subject, identifier);
        return new ProfileResponse(subject, profiles);
    }

}
