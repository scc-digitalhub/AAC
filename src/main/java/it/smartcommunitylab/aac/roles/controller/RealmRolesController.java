/*******************************************************************************
 * Copyright 2015 - 2021 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.roles.controller;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.roles.RealmRoleManager;
import it.smartcommunitylab.aac.roles.model.RealmRole;

@RestController
@Tag(name = "AAC Roles")
public class RealmRolesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RealmRoleManager roleManager;

    @Autowired
    private SubjectService subjectService;

    @Operation(summary = "Get roles of the subject in source realm")
    @PreAuthorize("(hasAuthority('" + Config.R_USER + "') and hasAuthority('SCOPE_" + Config.SCOPE_USER_ROLE
            + "')) or (hasAuthority('" + Config.R_CLIENT + "') and hasAuthority('SCOPE_" + Config.SCOPE_CLIENT_ROLE
            + "'))")
    @RequestMapping(method = RequestMethod.GET, value = "/roles/me")
    public Collection<RealmRole> getSubjectRoles(BearerTokenAuthentication auth)
            throws InvalidDefinitionException, NoSuchSubjectException {
        if (auth == null) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        String subject = (String) auth.getTokenAttributes().get("sub");

        if (!StringUtils.hasText(subject)) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        // return all the subject roles
        Subject sub = subjectService.getSubject(subject);

        return roleManager.getSubjectRoles(sub.getRealm(), subject);
    }
}
