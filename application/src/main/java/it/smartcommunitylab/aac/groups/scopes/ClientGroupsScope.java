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

package it.smartcommunitylab.aac.groups.scopes;

import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.scope.Scope;

public class ClientGroupsScope extends Scope {

    public static final String SCOPE = "client.groups.me";

    @Override
    public String getResourceId() {
        return GroupsResource.RESOURCE_ID;
    }

    @Override
    public ScopeType getType() {
        return ScopeType.CLIENT;
    }

    @Override
    public String getScope() {
        return SCOPE;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read client's groups";
    }

    @Override
    public String getDescription() {
        return "Groups of the current client. Read access only.";
    }
}
