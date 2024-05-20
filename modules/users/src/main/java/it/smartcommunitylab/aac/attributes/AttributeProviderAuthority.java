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

package it.smartcommunitylab.aac.attributes;

import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.attributes.provider.AttributeProviderSettingsMap;
import it.smartcommunitylab.aac.attributes.provider.UserAttributeProvider;
import it.smartcommunitylab.aac.attributes.provider.UserAttributeProviderConfig;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.model.ConfigMap;

public interface AttributeProviderAuthority<
    S extends UserAttributeProvider<? extends UserAttributes, M, C>,
    C extends UserAttributeProviderConfig<M>,
    M extends ConfigMap
>
    extends ConfigurableProviderAuthority<S, C, AttributeProviderSettingsMap, M> {
    //
    //    /*
    //     * identify
    //     */
    //    public String getAuthorityId();
    //
    //    /*
    //     * Providers
    //     */
    //    public boolean hasAttributeProvider(String providerId);
    //
    //    public AttributeProvider getAttributeProvider(String providerId) throws NoSuchProviderException;
    //
    //    public List<AttributeProvider> getAttributeProviders(String realm);
    //
    //    /*
    //     * Manage providers
    //     *
    //     * we expect providers to be registered and usable, or removed. To update config
    //     * implementations should unregister+register
    //     */
    //    public AttributeProvider registerAttributeProvider(ConfigurableAttributeProvider cp)
    //            throws IllegalArgumentException, RegistrationException, SystemException;
    //
    //    public void unregisterAttributeProvider(String providerId) throws SystemException;
    //
    //    /*
    //     * Services
    //     */
    //    public AttributeService getAttributeService(String providerId);
    //
    //    public List<AttributeService> getAttributeServices(String realm);

    /*
     * Config provider exposes configuration validation and schema
     */
    //    public AttributeConfigurationProvider<C, P> getConfigurationProvider();
}
