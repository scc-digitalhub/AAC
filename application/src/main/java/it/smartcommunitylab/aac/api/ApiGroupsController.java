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

package it.smartcommunitylab.aac.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.smartcommunitylab.aac.api.scopes.ApiGroupsScope;
import it.smartcommunitylab.aac.groups.BaseGroupController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@ApiSecurityTag(ApiGroupsScope.SCOPE)
@Tag(name = "Groups", description = "Manage realm groups and group membership")
@ApiRequestMapping
public class ApiGroupsController extends BaseGroupController {

    /*
     * API controller requires a specific scope.
     *
     * User permissions are handled at manager level.
     */
    private static final String AUTHORITY = "SCOPE_" + ApiGroupsScope.SCOPE;

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }
}
