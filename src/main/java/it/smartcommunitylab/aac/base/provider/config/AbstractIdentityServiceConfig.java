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

package it.smartcommunitylab.aac.base.provider.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.provider.config.IdentityServiceConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityServiceConfig;
import org.springframework.util.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    { @Type(value = InternalIdentityServiceConfig.class, name = InternalIdentityServiceConfig.RESOURCE_TYPE) }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.ALWAYS)
public abstract class AbstractIdentityServiceConfig<M extends AbstractConfigMap>
    extends AbstractProviderConfig<M, ConfigurableIdentityService>
    implements IdentityServiceConfig<M> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String repositoryId;

    protected AbstractIdentityServiceConfig(String authority, String provider, String realm, M configMap) {
        super(authority, provider, realm, configMap);
    }

    protected AbstractIdentityServiceConfig(ConfigurableIdentityService cp, M configMap) {
        super(cp, configMap);
        this.repositoryId = cp.getRepositoryId();
    }

    public String getRepositoryId() {
        // if undefined always use realm as default repository id
        return StringUtils.hasText(repositoryId) ? repositoryId : getRealm();
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }
}
