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

/*
 * SPID "Purpose" SAML extension (<spid:Purpose>, AgID Avviso SPID n.18 v.3): declared in the AuthnRequest to tell the
 * IdP which kinds of digital identity the service provider accepts. AAC only carries the value, the IdP enforces it.
 * Codes: PF = uso professionale della persona fisica, PG = uso professionale per la persona giuridica,
 * LP = persona giuridica or PG, P = PF or PG, PX = persona giuridica, PF or PG; the *S variants also accept the
 * corresponding identities for foreigners. No Purpose = classic SPID (persona fisica / persona giuridica only).
 */
public enum SpidPurpose {
    P("P"), // uso professionale, persona fisica o persona giuridica
    LP("LP"), // persona giuridica o uso professionale per la persona giuridica
    PG("PG"), // uso professionale per la persona giuridica
    PF("PF"), // uso professionale della persona fisica
    PX("PX"), // persona giuridica o uso professionale (persona fisica o persona giuridica)
    PS("PS"), // come P, anche per stranieri
    LPS("LPS"), // come LP, anche per stranieri
    PGS("PGS"), // come PG, anche per stranieri
    PFS("PFS"), // come PF, anche per stranieri
    PXS("PXS"); // come PX, anche per stranieri

    public static final String NAMESPACE_URI = "https://spid.gov.it/saml-extensions";
    public static final String NAMESPACE_PREFIX = "spid";
    public static final String ELEMENT_LOCAL_NAME = "Purpose";

    private final String value;

    SpidPurpose(String value) {
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

    public static SpidPurpose parse(String value) {
        for (SpidPurpose t : SpidPurpose.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
