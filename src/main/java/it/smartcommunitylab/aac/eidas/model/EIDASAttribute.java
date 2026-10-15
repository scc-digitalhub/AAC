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
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * The attributes an eidas provider can request: the catalogue of the eidas module, separate from the
 * SPID one and smaller.
 *
 * AAC talks SPID to the italian eIDAS node, so the attributes are named as the node releases them to
 * a Service Provider, i.e. with the SPID names (spidCode, name, familyName...). The translation from
 * the eIDAS profile is the node's job, not ours. This enum is therefore a SUBSET of SpidAttribute:
 * the SPID attributes that have a counterpart in what a node releases. Everything else in SpidAttribute
 * (company data, domicile, idCard, expirationDate, digitalAddress...) is SPID-only anagraphics that no
 * eIDAS node has: requesting it would make every assertion fail areRequestedAttributesObtained
 * (SPID 1013), so it is not offered and, if configured, refused at registration.
 *
 * The catalogue is the Minimum Data Set for natural persons of the annex to Reg. (EU) 2015/1501, and
 * nothing else. The four MANDATORY ones (identifier, given name, family name, date of birth) are
 * released by every notified scheme at every authentication and are the minimum a provider must
 * request. The three OPTIONAL ones of the same annex (place of birth, current address, gender) are
 * released by some schemes only: the assertion validator inherited from SPID (error 1013) rejects a
 * Response missing any requested attribute, so a provider requesting them accepts that citizens of
 * schemes without them cannot log in.
 *
 * Not in the catalogue, on purpose. The eIDAS SAML Attribute Profile (v1.4) also defines optional
 * additional attributes (Nationality, CountryOfBirth, TownOfBirth, CountryOfResidence, PhoneNumber,
 * EmailAddress): no scheme is required to release them and almost none does, so they are not offered.
 * A fiscal number does not exist for natural persons in any eIDAS profile, and foreign citizens have
 * none: fiscalNumber is not an eidas attribute. A test node must release what the rules say, not the
 * other way round.
 *
 * The identifier is spidCode: it is the only SPID attribute fit to carry an identifier that is not an
 * italian fiscal number (the node releases the eIDAS PersonIdentifier under that name).
 *
 * Verified on the italian node, pre-production (sp-proxy.pre.eid.gov.it, 2026-10-05): an AuthnRequest
 * whose AttributeConsumingService lists spidCode, name, familyName, dateOfBirth is forwarded by the
 * node to the eIDAS Connector as RequestedAttributes PersonIdentifier, CurrentGivenName,
 * CurrentFamilyName, DateOfBirth, all isRequired="true". The three optional ones are not observed yet.
 */
public enum EIDASAttribute {
    SPID_CODE(SpidAttribute.SPID_CODE, true, true),
    NAME(SpidAttribute.NAME, true, true),
    FAMILY_NAME(SpidAttribute.FAMILY_NAME, true, true),
    DATE_OF_BIRTH(SpidAttribute.DATE_OF_BIRTH, true, true),
    PLACE_OF_BIRTH(SpidAttribute.PLACE_OF_BIRTH, false, true),
    ADDRESS(SpidAttribute.ADDRESS, false, true),
    GENDER(SpidAttribute.GENDER, false, true);

    /*
     * The node releases every attribute twice, with the SPID name and with the eIDAS one under this
     * namespace. The second form is not part of the SPID dialect AAC speaks and is dropped from the
     * principal (EIDASUserAuthenticatedPrincipal).
     */
    public static final String NATURAL_PERSON_NAMESPACE = "http://eidas.europa.eu/attributes/naturalperson";

    private final SpidAttribute alias;
    private final boolean mandatory;
    private final boolean minimumDataSet;

    EIDASAttribute(SpidAttribute alias, boolean mandatory, boolean minimumDataSet) {
        this.alias = alias;
        this.mandatory = mandatory;
        this.minimumDataSet = minimumDataSet;
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

    /* the SPID attribute for the SPID stack: metadata, assertion checks */
    public SpidAttribute getAlias() {
        return alias;
    }

    /* true for the four attributes every node always releases: the minimum a provider must request */
    public boolean isMandatory() {
        return mandatory;
    }

    /* true when listed in the annex to Reg. (EU) 2015/1501 (mandatory or optional): always, by construction */
    public boolean isMinimumDataSet() {
        return minimumDataSet;
    }

    /*
     * Jackson: accepts the SPID name (case insensitive) and, leniently, the enum constant name. Anything
     * else - including SPID attributes outside this catalogue - fails deserialization, so the
     * configuration is refused at registration.
     */
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

    /* the four every provider must request */
    public static Set<EIDASAttribute> mandatory() {
        return EnumSet.allOf(EIDASAttribute.class).stream()
            .filter(EIDASAttribute::isMandatory)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(EIDASAttribute.class)));
    }

    /* the SPID attributes of the given ones, i.e. what goes into the SPID stack */
    public static Set<SpidAttribute> toSpidAttributes(Collection<EIDASAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return EnumSet.noneOf(SpidAttribute.class);
        }
        return attributes.stream().map(EIDASAttribute::getAlias).collect(Collectors.toCollection(() -> EnumSet.noneOf(SpidAttribute.class)));
    }

    /* the SPID attributes an eidas provider may request */
    public static Set<SpidAttribute> supportedAliases() {
        return toSpidAttributes(EnumSet.allOf(EIDASAttribute.class));
    }
}
