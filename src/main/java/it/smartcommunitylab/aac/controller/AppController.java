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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.UserManager;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for performing the basic operations over the client apps.
 * 
 * @author raman
 *
 */
@ApiIgnore
@Controller
public class AppController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserManager userManager;
    
    /**
     * Retrieve the with the user data: currently on the username is added.
     * 
     * @return
     */
  
    /**
     * Retrieve the with the user data: currently on the username is added.
     * 
     * @return
     */


    /**
     * Retrieve the with the user data: currently on the username is added.
     * 
     * @return
     */


}