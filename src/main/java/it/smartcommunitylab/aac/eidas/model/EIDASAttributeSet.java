/*
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

package it.smartcommunitylab.aac.eidas.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/*
 * The two attribute sets a Service Provider can ask the italian eIDAS node for, as fixed by AgID,
 * "Progetto FICEP - nodo eIDAS italiano, Avviso n. 01-2018", par. 4:
 *
 *  - index 99, "eIDAS Natural Person Minimum Attribute Set": spidCode, name, familyName, dateOfBirth
 *  - index 100, "eIDAS Natural Person Full Attribute Set": the four above plus placeOfBirth, address,
 *    gender
 *
 * Index, ServiceName and attributes are prescribed, not chosen: the SP metadata MUST declare both
 * AttributeConsumingService elements (par. 4), and an AuthnRequest may carry nothing but translatable
 * attributes (par. 5, table 1). So an eidas provider is not configured with a list of attributes but
 * with one of these two sets (EIDASIdentityProviderConfigMap.attributeSet, minimum by default): the
 * choice selects the AttributeConsumingServiceIndex of the AuthnRequest, the metadata always publish
 * both. Mind that the three attributes of the full set beyond the minimum are optional in the eIDAS
 * profile (table 1): a scheme that lacks them omits them, and the assertion validator inherited from
 * SPID rejects that Response.
 */
public enum EIDASAttributeSet {
    MINIMUM(
        "minimum",
        99,
        "eIDAS Natural Person Minimum Attribute Set",
        EnumSet.of(EIDASAttribute.SPID_CODE, EIDASAttribute.NAME, EIDASAttribute.FAMILY_NAME, EIDASAttribute.DATE_OF_BIRTH)
    ),
    FULL(
        "full",
        100,
        "eIDAS Natural Person Full Attribute Set",
        EnumSet.allOf(EIDASAttribute.class));

    private final String value;
    private final int index;
    private final String serviceName;
    private final Set<EIDASAttribute> attributes;

    EIDASAttributeSet(String value, int index, String serviceName, Set<EIDASAttribute> attributes) {
        this.value = value;
        this.index = index;
        this.serviceName = serviceName;
        this.attributes = Collections.unmodifiableSet(attributes);
    }

    /* the configuration value: minimum, full */
    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    /* the AttributeConsumingService index prescribed for this set: 99, 100 */
    public int getIndex() {
        return index;
    }

    /* the ServiceName prescribed for this set */
    public String getServiceName() {
        return serviceName;
    }

    /* the attributes of this set, in catalogue order */
    public Set<EIDASAttribute> getAttributes() {
        return attributes;
    }

    /* Jackson: accepts the value (case insensitive) and, leniently, the enum constant name */
    @JsonCreator
    public static EIDASAttributeSet parse(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        for (EIDASAttributeSet set : values()) {
            if (set.value.equalsIgnoreCase(v) || set.name().equalsIgnoreCase(v)) {
                return set;
            }
        }
        throw new IllegalArgumentException("unknown eidas attribute set: " + value);
    }
}
