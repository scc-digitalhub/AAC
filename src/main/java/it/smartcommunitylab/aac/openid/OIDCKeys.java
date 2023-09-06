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

package it.smartcommunitylab.aac.openid;

import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

public class OIDCKeys {

    static final String[] ACCOUNT_ATTRIBUTES_VALUES = {
        "username",
        OpenIdAttributesSet.NAME,
        OpenIdAttributesSet.FAMILY_NAME,
        OpenIdAttributesSet.GIVEN_NAME,
        OpenIdAttributesSet.PREFERRED_USERNAME,
        OpenIdAttributesSet.EMAIL,
        OpenIdAttributesSet.EMAIL_VERIFIED,
        OpenIdAttributesSet.PICTURE,
        OpenIdAttributesSet.LOCALE,
    };

    static final String[] JWT_ATTRIBUTES_VALUES = {
        IdTokenClaimNames.ACR,
        IdTokenClaimNames.AMR,
        IdTokenClaimNames.AT_HASH,
        IdTokenClaimNames.AUD,
        IdTokenClaimNames.AUTH_TIME,
        IdTokenClaimNames.AZP,
        IdTokenClaimNames.C_HASH,
        IdTokenClaimNames.EXP,
        IdTokenClaimNames.IAT,
        IdTokenClaimNames.ISS,
        IdTokenClaimNames.NONCE,
        IdTokenClaimNames.SUB,
    };

    public static final Set<String> JWT_ATTRIBUTES;
    public static final Set<String> ACCOUNT_ATTRIBUTES;

    static {
        JWT_ATTRIBUTES = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(JWT_ATTRIBUTES_VALUES)));
        ACCOUNT_ATTRIBUTES = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(ACCOUNT_ATTRIBUTES_VALUES)));
    }
}
