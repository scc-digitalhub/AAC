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

package it.smartcommunitylab.aac.profiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.common.SecurityUtils;
import it.smartcommunitylab.aac.dto.AccountProfile;
import it.smartcommunitylab.aac.dto.BasicProfile;
import it.smartcommunitylab.aac.manager.ProfileManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ErrorInfo;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.model.User;

/**
 * @author raman
 *
 */
@Controller
@Api(tags = { "AAC User profile" })
public class BasicProfileController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserManager userManager;

    @Autowired
    private ProfileManager profileManager;

    @ApiOperation(value = "Get basic profile of a user")
    @RequestMapping(method = RequestMethod.GET, value = "/basicprofile/all/{userId}")
    public @ResponseBody BasicProfile getUser(@PathVariable("userId") Long userId,
            Authentication auth,
            HttpServletResponse response) throws IOException {

        String clientId = SecurityUtils.getOAuthClientId(auth);
        logger.debug("get basic profile for user " + String.valueOf(userId) + " from client " + clientId);

        // will trigger exception if user does not exists
        User user = userManager.getOne(userId);
        userId = user.getId();

        return profileManager.getBasicProfileById(Long.toString(userId));
    }

    @ApiOperation(value = "Get basic profile of all users")
    @RequestMapping(method = RequestMethod.GET, value = "/basicprofile/all")
    public @ResponseBody List<BasicProfile> searchUsers(
            @RequestParam(value = "filter", required = false) String fullNameFilter,
            @RequestParam(value = "username", required = false) String userName,
            Authentication auth,
            HttpServletResponse response) throws IOException {

        String clientId = SecurityUtils.getOAuthClientId(auth);
        logger.debug("get basic profile for all users from client " + clientId);

        List<BasicProfile> list = new ArrayList<>();
        if (!StringUtils.isEmpty(userName)) {
            list.add(profileManager.getUser(userName));
        } else if (!StringUtils.isEmpty(fullNameFilter)) {
            list.addAll(profileManager.getUsers(fullNameFilter));
        } else {
            list.addAll(profileManager.getUsers());
        }

        return list;
    }

    @ApiOperation(value = "Get basic profile of the current user")
    @RequestMapping(method = RequestMethod.GET, value = "/basicprofile/me")
    public @ResponseBody BasicProfile findProfile(Authentication auth, HttpServletResponse response)
            throws IOException {

        String clientId = SecurityUtils.getOAuthClientId(auth);
        String userId = SecurityUtils.getOAuthUserId(auth);

        logger.debug("get basic profile for user " + userId + " from client " + clientId);

        // will trigger exception if user does not exists
        User user = userManager.getOne(Long.parseLong(userId));
        userId = Long.toString(user.getId());

        return profileManager.getBasicProfileById(userId);
    }

    @ApiOperation(value = "Get basic profile of specified users")
    @RequestMapping(method = RequestMethod.GET, value = "/basicprofile/profiles")
    public @ResponseBody List<BasicProfile> findProfiles(@RequestParam List<String> userIds,
            Authentication auth,
            HttpServletResponse response) {

        String clientId = SecurityUtils.getOAuthClientId(auth);

        logger.debug("get basic profile for users " + userIds.toString() + " from client " + clientId);

        // filter null profiles
        return profileManager.getUsers(userIds).stream()
                .filter(p -> (p != null))
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "Get account data of a current user")
    @RequestMapping(method = RequestMethod.GET, value = "/accountprofile/me")
    public @ResponseBody AccountProfile findAccountProfile(Authentication auth, HttpServletResponse response) {
        String clientId = SecurityUtils.getOAuthClientId(auth);
        String userId = SecurityUtils.getOAuthUserId(auth);

        logger.debug("get account profile for user " + userId + " from client " + clientId);

        // will trigger exception if user does not exists
        User user = userManager.getOne(Long.parseLong(userId));
        userId = Long.toString(user.getId());

        return profileManager.getAccountProfileById(userId);
    }

    @ApiOperation(value = "Get account profiles of specified users")
    @RequestMapping(method = RequestMethod.GET, value = "/accountprofile/profiles")
    public @ResponseBody List<AccountProfile> findAccountProfiles(@RequestParam List<String> userIds,
            Authentication auth, HttpServletResponse response) throws IOException {

        String clientId = SecurityUtils.getOAuthClientId(auth);

        logger.debug("get account profile for users " + userIds.toString() + " from client " + clientId);

        // filter null profiles
        return profileManager.getAccountProfilesById(userIds).stream()
                .filter(p -> (p != null))
                .collect(Collectors.toList());
    }

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
