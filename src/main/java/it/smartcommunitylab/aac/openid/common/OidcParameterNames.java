/**
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

package it.smartcommunitylab.aac.openid.common;

public interface OidcParameterNames {
    /**
     * {@code nonce} - String value used to associate a Client session with an ID Token, and to mitigate replay attacks
     */
    String NONCE = "nonce";

    /**
     * {@code display} - ASCII string value that specifies how the Authorization Server displays the authentication and consent user interface pages to the End-User
     */
    String DISPLAY = "display";

    /**
     * {@code prompt} - Space-delimited, case-sensitive list of ASCII string values that specifies whether the Authorization Server prompts the End-User for reauthentication and consent
     */
    String PROMPT = "prompt";

    /**
     * {@code max_age} - Maximum Authentication Age
     */
    String MAX_AGE = "max_age";

    /**
     * {@code ui_locales} - End-User's preferred languages and scripts for the user interface, represented as a space-separated list of BCP47 [RFC5646] language tag values, ordered by preference.
     */
    String UI_LOCALES = "ui_locales";

    /**
     * {@code id_token_hint} - ID Token previously issued by the Authorization Server being passed as a hint about the End-User's current or past authenticated session with the Client
     */
    String ID_TOKEN_HINT = "id_token_hint";

    /**
     * {@code login_hint} - Hint to the Authorization Server about the login identifier the End-User might use to log in
     */
    String LOGIN_HINT = "login_hint";

    /**
     * {@code acr_values} - Requested Authentication Context Class Reference values
     */
    String ACR_VALUES = "acr_values";

    /**
     * {@code claims} Request specific Claims to be returned
     */
    String CLAIMS = "claims";

    /**
     * {@code claims_locales} - End-User's preferred languages and scripts for Claims being returned, represented as a space-separated list of BCP47 [RFC5646] language tag values
     */
    String CLAIMS_LOCALES = "claims_locales";

    /**
     * {@code request} - This parameter enables OpenID Connect requests to be passed in a single, self-contained parameter and to be optionally signed and/or encrypted
     */
    String REQUEST = "request";

    /**
     * {@code request_uri} - 	This parameter enables OpenID Connect requests to be passed by reference, rather than by value
     */
    String REQUEST_URI = "request_uri";
}
