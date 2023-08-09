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

package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import java.util.Collection;

public interface AttributeService<M extends ConfigMap, C extends AttributeProviderConfig<M>>
    extends AttributeProvider<M, C> {
    /*
     * Attribute management
     */

    public Collection<UserAttributes> putUserAttributes(String subjectId, Collection<AttributeSet> attributes);

    public UserAttributes putUserAttributes(String subjectId, String setId, AttributeSet attributes);
}
