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

package it.smartcommunitylab.aac.spid.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

public enum SpidAttribute {
    SPID_CODE("spidCode"),
    NAME("name"),
    FAMILY_NAME("familyName"),
    PLACE_OF_BIRTH("placeOfBirth"),
    COUNTRY_OF_BIRTH("countyOfBirth"),
    DATE_OF_BIRTH("dateOfBirth"),
    GENDER("gender"),
    COMPANY_NAME("companyName"),
    REGISTERED_OFFICE("registeredOffice"),
    FISCAL_NUMBER("fiscalNumber"),
    IVA_CODE("ivaCode"),
    ID_CARD("idCard"),
    MOBILE_PHONE("mobilePhone"),
    EMAIL("email"),
    DOMICILE_STREET_ADDRESS("domicileStreetAddress"),
    DOMICILE_POSTAL_CODE("domicilePostalCode"),
    DOMICILE_MUNICIPALITY("domicileMunicipality"),
    DOMICILE_PROVINCE("domicileProvince"),
    ADDRESS("address"),
    DOMICILE_NATION("domicileNation"),
    EXPIRATION_DATE("expirationDate"),
    DIGITAL_ADDRESS("digitalAddress");

    private final String value;

    SpidAttribute(String value) {
        Assert.hasText(value, "value cannot be empty");
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String toString() {
        return value;
    }

    public static SpidAttribute parse(String value) {
        for (SpidAttribute t : SpidAttribute.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }

    public static boolean contains(String value) {
        for (SpidAttribute t : SpidAttribute.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
