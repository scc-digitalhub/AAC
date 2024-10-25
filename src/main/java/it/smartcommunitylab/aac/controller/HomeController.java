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

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import java.io.IOException;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * Home controller
 *
 */
@Controller
public class HomeController {

    @RolesAllowed("ROLE_USER")
    @GetMapping("/")
    public ModelAndView home() {
        // NOTE: set fragment empty to make sure redirected requests via 3xx
        // do not keep their fragment
        // we use # for in-app routing in react, so we want a clean route
        return new ModelAndView("redirect:/console/user#");
    }

    @RolesAllowed("ROLE_USER")
    @GetMapping("/console/user")
    public ModelAndView userConsole() {
        return new ModelAndView("forward:/console/user/index.html");
    }

    @RolesAllowed("ROLE_USER")
    @GetMapping("/console/dev")
    public ModelAndView devConsole() {
        // return new ModelAndView("redirect:/dev");
        return new ModelAndView("redirect:/console/dev/");
    }

    @RolesAllowed("ROLE_USER")
    @GetMapping("/console/admin")
    public ModelAndView adminConsole() {
        // return new ModelAndView("redirect:/dev#/admin");
        // return new ModelAndView("forward:/console/admin/index.html");
        return new ModelAndView("redirect:/console/admin/");
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
