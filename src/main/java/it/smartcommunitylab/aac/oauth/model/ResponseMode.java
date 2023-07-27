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

package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.Assert;

/*
 * Response modes according to
 *
 * OAuth 2.0 Multiple Response Type Encoding Practices
 * https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html
 *
 * OAuth 2.0 Form Post Response Mode
 * https://openid.net/specs/oauth-v2-form-post-response-mode-1_0.html
 */

public enum ResponseMode {
    QUERY("query"),
    FRAGMENT("fragment"),
    FORM_POST("form_post");

    private final String value;

    ResponseMode(String value) {
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

    public static ResponseMode parse(String value) {
        for (ResponseMode t : ResponseMode.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
