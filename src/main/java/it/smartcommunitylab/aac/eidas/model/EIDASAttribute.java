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
import it.smartcommunitylab.aac.spid.model.SpidAttribute;

/*
 * The eidas catalogue: the attributes an eidas provider can request, a subset of SpidAttribute.
 *
 * AAC talks SPID to the italian node, so the attributes carry the SPID names the node releases them
 * under (spidCode, name, familyName...). They are the seven AgID lists as translatable to eIDAS
 * (Avviso eIDAS n. 01-2018, table 1): the Minimum Data Set for natural persons of Reg. (EU) 2015/1501.
 * The rest of SpidAttribute (fiscalNumber, email, company data...) no eIDAS node releases: requesting
 * it would fail every assertion (SPID 1013), so it is not offered and refused at registration.
 * Providers request one of the two prescribed sets (EIDASAttributeSet), never single attributes:
 * mandatory/optional is a property of the sets, the enum carries nothing but the SPID name.
 */
public enum EIDASAttribute {
    SPID_CODE(SpidAttribute.SPID_CODE),
    NAME(SpidAttribute.NAME),
    FAMILY_NAME(SpidAttribute.FAMILY_NAME),
    DATE_OF_BIRTH(SpidAttribute.DATE_OF_BIRTH),
    PLACE_OF_BIRTH(SpidAttribute.PLACE_OF_BIRTH),
    ADDRESS(SpidAttribute.ADDRESS),
    GENDER(SpidAttribute.GENDER);

    /* the node releases every attribute twice, SPID name and eIDAS name under this namespace; the second is dropped */
    public static final String NATURAL_PERSON_NAMESPACE = "http://eidas.europa.eu/attributes/naturalperson";

    private final SpidAttribute alias;

    EIDASAttribute(SpidAttribute alias) {
        this.alias = alias;
    }

    /* the configuration value: the SPID name (spidCode, name, familyName...) */
    @JsonValue
    public String getValue() {
        return alias.getValue();
    }

    @Override
    public String toString() {
        return getValue();
    }

    /* the SPID name the node releases this attribute under: the only SPID-facing side of the catalogue */
    public SpidAttribute getAlias() {
        return alias;
    }

    /* Jackson: the SPID name (case insensitive) or the constant name; anything else is refused at registration */
    @JsonCreator
    public static EIDASAttribute parse(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        for (EIDASAttribute a : values()) {
            if (a.getValue().equalsIgnoreCase(v) || a.name().equalsIgnoreCase(v)) {
                return a;
            }
        }
        throw new IllegalArgumentException("unknown eidas attribute: " + value);
    }

    /* the eidas attribute behind a SPID one, null when a node does not release it */
    public static EIDASAttribute fromAlias(SpidAttribute alias) {
        for (EIDASAttribute a : values()) {
            if (a.alias == alias) {
                return a;
            }
        }
        return null;
    }
}
