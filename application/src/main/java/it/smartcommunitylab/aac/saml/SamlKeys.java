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

package it.smartcommunitylab.aac.saml;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class SamlKeys {

    static final String[] SAML_ATTRIBUTES_VALUES = { "subject", "issuer", "issueInstant" };

    static final String[] ACCOUNT_ATTRIBUTES_VALUES = { "username", "name", "email", "locale" };

    public static final Set<String> SAML_ATTRIBUTES;
    public static final Set<String> ACCOUNT_ATTRIBUTES;

    static {
        SAML_ATTRIBUTES = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(SAML_ATTRIBUTES_VALUES)));
        ACCOUNT_ATTRIBUTES = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(ACCOUNT_ATTRIBUTES_VALUES)));
    }
}
