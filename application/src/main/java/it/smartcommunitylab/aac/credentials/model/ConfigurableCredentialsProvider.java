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

package it.smartcommunitylab.aac.credentials.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableProviderImpl;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceSettingsMap;
import javax.validation.Valid;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurableCredentialsProvider extends ConfigurableProviderImpl<CredentialsServiceSettingsMap> {

    public ConfigurableCredentialsProvider(String authority, String provider, String realm) {
        super(SystemKeys.RESOURCE_CREDENTIALS, authority, provider, realm);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private ConfigurableCredentialsProvider() {
        super(SystemKeys.RESOURCE_CREDENTIALS, null, null, null);
    }
}
