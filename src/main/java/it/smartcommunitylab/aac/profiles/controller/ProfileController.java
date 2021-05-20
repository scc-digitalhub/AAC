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
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiAuditScope;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.model.ErrorInfo;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.profiles.ProfileManager;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;

/**
 * @author raman
 *
 */
@RestController
@Api(tags = { "AAC User profile" })
public class ProfileController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProfileManager profileManager;

    @Autowired
    private AuthenticationHelper authHelper;

    /*
     * Current user operations TODO evaluate move/copy to MVC controller and keep
     * this API only, with bearer token
     */

    @ApiOperation(value = "Get basic profile of the current user")
    @PreAuthorize("hasAuthority('" + Config.R_USER + "') and hasAuthority('SCOPE_" + Config.SCOPE_BASIC_PROFILE + "')")
    @GetMapping(value = "/basicprofile/me")
    public @ResponseBody BasicProfile myBasicProfile(BearerTokenAuthentication auth)
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

        return profileManager.getBasicProfile(realm, subject);
    }

    @ApiOperation(value = "Get openid profile of the current user")
    @PreAuthorize("hasAuthority('" + Config.R_USER + "') and hasAuthority('SCOPE_" + Config.SCOPE_PROFILE + "')")
    @GetMapping(value = "/openidprofile/me")
    public @ResponseBody OpenIdProfile myOpenIdProfile(BearerTokenAuthentication auth)
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

        return profileManager.getOpenIdProfile(realm, subject);
    }

    @ApiOperation(value = "Get account profiles of the current user")
    @PreAuthorize("hasAuthority('" + Config.R_USER + "') and hasAuthority('SCOPE_" + Config.SCOPE_ACCOUNT_PROFILE
            + "')")
    @GetMapping(value = "/accountprofile/me")
    public @ResponseBody Collection<AccountProfile> myAccountProfiles(BearerTokenAuthentication auth)
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

        return profileManager.getAccountProfiles(realm, subject);
    }

    /*
     * Debug
     */

    @RolesAllowed("ROLE_USER")
    @GetMapping(value = "/whoami")
    public @ResponseBody UserAuthenticationToken debug(Authentication auth, HttpServletResponse response)
            throws IOException {

        // authentication should be a user authentication
        if (!(auth instanceof UserAuthenticationToken)) {
            throw new InsufficientAuthenticationException("not a user authentication");
        }

        UserAuthenticationToken token = (UserAuthenticationToken) auth;

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
    @ApiOperation(value = "Get basic profile of a user")
    @GetMapping(value = "/api/profiles/{realm}/basicprofile/{userId}")
    public @ResponseBody BasicProfile getBasicProfile(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException, InvalidDefinitionException {
        return profileManager.getBasicProfile(realm, userId);
    }

    @ApiOperation(value = "Get openid profile of a user")
    @GetMapping(value = "/api/profiles/{realm}/openidprofile/{userId}")
    public @ResponseBody OpenIdProfile getOpenIdProfile(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException, InvalidDefinitionException {
        return profileManager.getOpenIdProfile(realm, userId);
    }

    @ApiOperation(value = "Get account profiles of a user")
    @GetMapping(value = "/api/profiles/{realm}/accountprofile/{userId}")
    public @ResponseBody Collection<AccountProfile> getAccountProfiles(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String userId)
            throws NoSuchRealmException, NoSuchUserException, InvalidDefinitionException {
        return profileManager.getAccountProfiles(realm, userId);
    }

    /*
     * Exceptions
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    Response processNotFoundError(EntityNotFoundException ex) {
        return Response.error(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    Response processNotAcceptableError(MethodArgumentTypeMismatchException ex) {
        return Response.error(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    Response processAccessError(AccessDeniedException ex) {
        return Response.error(ex.getMessage());
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    Response processAuthError(InsufficientAuthenticationException ex) {
        return Response.error(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Response processValidationError(IllegalArgumentException ex) {
        return Response.error(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    @ResponseBody
    ErrorInfo handleBadRequest(HttpServletRequest req, Exception ex) {
        StackTraceElement ste = ex.getStackTrace()[0];
        return new ErrorInfo(req.getRequestURL().toString(), ex.getClass().getTypeName(), ste.getClassName(),
                ste.getLineNumber());
    }

}
