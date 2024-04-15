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

package it.smartcommunitylab.aac.attributes.model;

import it.smartcommunitylab.aac.model.AttributeType;
import java.io.Serializable;

/*
 * An attribute is a typed property describing a value for a given resource.
 *
 * While unusual, attributes may assume multiple values.
 */

public interface Attribute extends Serializable {
    public String getKey();

    public AttributeType getType();

    public Serializable getValue();

    public String exportValue();

    public String getName();

    public String getDescription();

    public Boolean getIsMultiple();
}
