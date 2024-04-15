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

package it.smartcommunitylab.aac.scope;

import java.util.Collection;

/*
 * Scope provider defines a set of scopes related to a service/implementation etc.
 *
 * The system will fetch scope definitions from providers and populate the registry when needed.
 *
 * Do note that we expect exported scopes to match the resourceId declared from provider
 */
public interface ScopeProvider {
    public String getResourceId();

    public Resource getResource();

    public Collection<Scope> getScopes();

    public ScopeApprover getApprover(String scope);
}
