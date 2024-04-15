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

package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.model.Resource;

/*
 * A provider for resources of a given type, for a specific authority
 */
public interface ResourceProvider<R extends Resource> {
    /*
     * identify this provider
     */
    public String getId();

    public String getAuthority();

    public String getRealm();

    default String getProvider() {
        return getId();
    }
}
