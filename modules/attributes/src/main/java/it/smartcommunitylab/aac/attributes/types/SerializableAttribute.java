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

package it.smartcommunitylab.aac.attributes.types;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.base.AbstractAttribute;
import it.smartcommunitylab.aac.attributes.model.AttributeType;
import java.io.Serializable;

/*
 * An attribute type supporting OBJECT in serializable form.
 *
 * We let type resettable but we expect the definition to match the content.
 * Changing the type will render this attribute opaque for service.
 */
public class SerializableAttribute extends AbstractAttribute {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    private AttributeType type;

    private Serializable value;

    public SerializableAttribute(String key) {
        this.key = key;
        this.type = AttributeType.OBJECT;
    }

    public SerializableAttribute(String key, Serializable value) {
        this.key = key;
        this.value = value;
        this.type = AttributeType.OBJECT;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }
}
