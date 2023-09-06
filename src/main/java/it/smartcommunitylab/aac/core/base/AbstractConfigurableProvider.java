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

package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfig;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.Assert;

public abstract class AbstractConfigurableProvider<
    R extends Resource, T extends ConfigurableProvider, M extends ConfigMap, C extends ProviderConfig<M>
>
    extends AbstractProvider<R>
    implements ConfigurableResourceProvider<R, T, M, C> {

    protected final C config;

    protected AbstractConfigurableProvider(String authority, String provider, String realm, C providerConfig) {
        super(authority, provider, realm);
        Assert.notNull(providerConfig, "provider config can not be null");

        // check configuration
        Assert.isTrue(authority.equals(providerConfig.getAuthority()), "configuration does not match this provider");
        Assert.isTrue(provider.equals(providerConfig.getProvider()), "configuration does not match this provider");
        Assert.isTrue(realm.equals(providerConfig.getRealm()), "configuration does not match this provider");

        this.config = providerConfig;
    }

    @Override
    public C getConfig() {
        return config;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getTitle(Locale locale) {
        String lang = locale.getLanguage();
        if (config.getTitleMap() != null) {
            return config.getTitleMap().get(lang);
        }

        return null;
    }

    @Override
    public String getDescription(Locale locale) {
        String lang = locale.getLanguage();
        if (config.getDescriptionMap() != null) {
            return config.getDescriptionMap().get(lang);
        }

        return null;
    }

    public Map<String, String> getTitleMap() {
        return config.getTitleMap();
    }

    public Map<String, String> getDescriptionMap() {
        return config.getDescriptionMap();
    }
}
