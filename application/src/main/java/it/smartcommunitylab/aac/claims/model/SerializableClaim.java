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

package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.model.AttributeType;
import java.io.Serializable;

/*
 * A claim type supporting OBJECT in serializable form.
 * Key is optional, to merge with other claims under the namespace set null
 *
 * We let type resettable but we expect the definition to match the content.
 * Changing the type will render this claim opaque for service.
 */
public class SerializableClaim extends AbstractClaim {

    private AttributeType type;

    private Serializable value;

    public SerializableClaim(String key) {
        this.key = key;
        this.type = AttributeType.OBJECT;
    }

    public SerializableClaim(String key, Serializable value) {
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
