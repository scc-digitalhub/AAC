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
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.core.UserAuthenticationToken;
import it.smartcommunitylab.aac.model.ErrorInfo;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

/**
 * @author raman
 *
 */
@Controller
@Api(tags = { "AAC User profile" })
public class BasicProfileController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ApiOperation(value = "Get basic profile of the current user")
    @RequestMapping(method = RequestMethod.GET, value = "/basicprofile/me")
    public @ResponseBody BasicProfile findProfile(Authentication auth, HttpServletResponse response)
            throws IOException {

        // authentication should be a user authentication
        if (!(auth instanceof UserAuthenticationToken)) {
            throw new InsufficientAuthenticationException("not a user authentication");
        }

        UserAuthenticationToken token = (UserAuthenticationToken) auth;

        BasicProfile profile = token.getUser().getBasicProfile();
        return profile;

    }

    @RequestMapping(method = RequestMethod.GET, value = "/whoami")
    public @ResponseBody UserAuthenticationToken debug(Authentication auth, HttpServletResponse response)
            throws IOException {

        // authentication should be a user authentication
        if (!(auth instanceof UserAuthenticationToken)) {
            throw new InsufficientAuthenticationException("not a user authentication");
        }

        UserAuthenticationToken token = (UserAuthenticationToken) auth;

        return token;

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
