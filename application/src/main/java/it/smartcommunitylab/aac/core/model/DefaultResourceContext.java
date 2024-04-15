/**
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

@JsonInclude(Include.NON_NULL)
public class DefaultResourceContext<R extends Resource> implements ResourceContext<R> {

    // resources stored as map context and read via accessors
    private final Map<String, List<? extends R>> resources;

    public DefaultResourceContext(Map<String, List<? extends R>> resources) {
        Assert.notNull(resources, "resources map can not be null");
        this.resources = resources;
    }

    @Override
    public Map<String, List<? extends R>> getResources() {
        return resources;
    }
}
