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

package it.smartcommunitylab.aac.oauth.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.scope.Scope;

public class OAuth2DCRScope extends Scope {

    public static final String SCOPE = Config.SCOPE_DYNAMIC_CLIENT_REGISTRATION;

    @Override
    public String getResourceId() {
        return OAuth2DCRResource.RESOURCE_ID;
    }

    @Override
    public ScopeType getType() {
        return ScopeType.GENERIC;
    }

    @Override
    public String getScope() {
        return SCOPE;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Dynamic client registration";
    }

    @Override
    public String getDescription() {
        return "Dynamic client registration for OAuth2/OIDC";
    }
}
