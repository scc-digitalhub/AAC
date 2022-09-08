/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
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
 ******************************************************************************/

package it.smartcommunitylab.aac.controller;

import java.io.IOException;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;

/**
 * Home controller
 *
 */
@Controller
public class HomeController {

    @RolesAllowed("ROLE_USER")
    @GetMapping("/")
    public ModelAndView home() {
        return new ModelAndView("redirect:/account");
    }

    /*
     * Debug
     * 
     * TODO remove
     */

    @RolesAllowed("ROLE_USER")
    @GetMapping(value = "/whoami")
    @Hidden
    public @ResponseBody UserAuthentication debug(Authentication auth, HttpServletResponse response)
            throws IOException {

        // authentication should be a user authentication
        if (!(auth instanceof UserAuthentication)) {
            throw new InsufficientAuthenticationException("not a user authentication");
        }

        UserAuthentication token = (UserAuthentication) auth;

        return token;

    }

}
