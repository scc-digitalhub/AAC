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

package it.smartcommunitylab.aac.templates;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.templates.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.templates.model.Template;
import it.smartcommunitylab.aac.templates.provider.TemplateProvider;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderSettingsMap;

public interface TemplateProviderAuthority<
    S extends TemplateProvider<T, M, C>, T extends Template, M extends ConfigMap, C extends TemplateProviderConfig<M>
>
    extends ConfigurableProviderAuthority<S, ConfigurableTemplateProvider, C, TemplateProviderSettingsMap, M> {
    public S findProviderByRealm(String realm);

    public S getProviderByRealm(String realm) throws NoSuchProviderException;
}
