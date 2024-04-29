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

package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.io.Serializable;
import java.util.Map;

/*
 * Expose provider configuration outside modules
 * attached to authorities because different authorities could share the same config/provider
 */
public interface ConfigurationProvider<
    P extends ProviderConfig<S, M>, C extends ConfigurableProvider<S>, S extends ConfigMap, M extends ConfigMap
> {
    //configs are tied to an authority
    public String getAuthority();

    // //expose the config repository
    // public ProviderConfigRepository<P> getRepository();

    // public P register(C config) throws RegistrationException;

    // public void unregister(String providerId);

    // public default String getType() {
    //     return SystemKeys.RESOURCE_CONFIG;
    // }

    /*
     * Translate a configurableProvider with valid props to a valid provider config,
     * with default values set
     */
    public P getConfig(C cp);

    public P getConfig(C cp, boolean mergeDefault);

    /*
     * Translate a provider config to a configurable
     */
    public C getConfigurable(P providerConfig);

    /*
     * Expose and translate to valid configMap
     */

    public M getDefaultConfigMap();

    public M getConfigMap(Map<String, Serializable> map);

    /*
     * Expose and translate to valid settingsMap
     */

    public S getDefaultSettingsMap();

    public S getSettingsMap(Map<String, Serializable> map);
    /*
     * Validate configuration against schema and also policies (TODO)
     */
    //    public boolean isConfigValid(T cp);
    //
    //    public boolean isConfigMapValid(M configMap);

    /*
     * Export the configuration schema for configMap
     *
     * this schema should match M but could include descriptions, localized labels
     * etc and should be used for API doc, UI forms etc
     */

    // public JsonSchema getSchema();
}
