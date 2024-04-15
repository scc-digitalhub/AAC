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

package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.profiles.claims.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.Resource;

public class ProfileResource extends Resource {

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "User profile";
    }

    @Override
    public String getDescription() {
        return "Access user profile: basic, account, custom";
    }
}
