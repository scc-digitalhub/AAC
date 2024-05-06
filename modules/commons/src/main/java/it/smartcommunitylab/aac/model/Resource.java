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

package it.smartcommunitylab.aac.model;

import java.io.Serializable;

/*
 * A realm scoped resource, handled by an authority via a given provider
 */

public interface Resource extends Serializable {
    //resource context
    //TODO replace with a composite scope key
    public String getRealm();

    public String getAuthority();

    public String getProvider();

    // id is global across all resources (uuid)
    public String getId();

    // TODO replace with proper typing <T> on resource
    public String getType();

    //addressable key
    default String getKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(getType()).append("://");
        sb.append(getAuthority()).append("/");
        sb.append(getProvider()).append(":");
        sb.append(getId());

        return sb.toString();
    }
}
