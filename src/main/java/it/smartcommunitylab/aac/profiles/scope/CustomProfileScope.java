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

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class CustomProfileScope extends AbstractProfileScope {

    private final String identifier;

    public CustomProfileScope(String identifier) {
        Assert.hasText(identifier, "identifier can not be null");
        this.identifier = identifier;
        this.scope = "profile." + identifier + ".me";
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String getScope() {
        return scope;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read user's profile " + identifier;
    }

    @Override
    public String getDescription() {
        return StringUtils.capitalize(identifier) + " profile of the current platform user. Read access only.";
    }
}
