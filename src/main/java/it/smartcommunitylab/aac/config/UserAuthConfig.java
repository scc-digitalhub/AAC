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

package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.attributes.service.AttributeProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.identity.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.users.ExtendedUserAuthenticationManager;
import it.smartcommunitylab.aac.users.service.UserEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(15)
public class UserAuthConfig {

    @Autowired
    private IdentityProviderAuthorityService identityProviderAuthorityService;

    @Autowired
    private AttributeProviderAuthorityService attributeProviderAuthorityService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private UserEntityService userService;

    @Bean
    public ExtendedUserAuthenticationManager extendedAuthenticationManager() throws Exception {
        return new ExtendedUserAuthenticationManager(
            identityProviderAuthorityService,
            attributeProviderAuthorityService,
            userService,
            subjectService
        );
    }
}
