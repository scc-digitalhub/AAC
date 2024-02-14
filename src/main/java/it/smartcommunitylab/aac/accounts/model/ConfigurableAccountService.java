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

package it.smartcommunitylab.aac.accounts.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableProviderImpl;
import javax.validation.Valid;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurableAccountService extends ConfigurableProviderImpl<AccountServiceSettingsMap> {

    public ConfigurableAccountService(String authority, String provider, String realm) {
        super(SystemKeys.RESOURCE_ACCOUNT, authority, provider, realm);
    }

    public ConfigurableAccountService() {
        super(SystemKeys.RESOURCE_ACCOUNT, null, null, null);
    }
}
