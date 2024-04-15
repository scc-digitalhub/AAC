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

package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.authorities.ConfigurableAuthorityService;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProviderAuthorityService {

    private final Map<
        String,
        ConfigurableAuthorityService<? extends ConfigurableProviderAuthority<?, ?, ?, ?, ?>>
    > services;

    public ProviderAuthorityService(Collection<ConfigurableAuthorityService<?>> services) {
        this.services = services.stream().collect(Collectors.toMap(s -> s.getType(), s -> s));
    }

    public ConfigurableAuthorityService<? extends ConfigurableProviderAuthority<?, ?, ?, ?, ?>> findAuthorityService(
        String type
    ) {
        return services.get(type);
    }

    public ConfigurableAuthorityService<? extends ConfigurableProviderAuthority<?, ?, ?, ?, ?>> getAuthorityService(
        String type
    ) throws NoSuchProviderException {
        ConfigurableAuthorityService<?> as = findAuthorityService(type);
        if (as == null) {
            throw new NoSuchProviderException();
        }

        return as;
    }
    //    public ResourceProvider<?> registerResourceProvider(ConfigurableProvider cp)
    //            throws NoSuchAuthorityException, NoSuchProviderException {
    //        if (cp == null) {
    //            throw new IllegalArgumentException("invalid provider");
    //        }
    //
    //        if (!cp.isEnabled()) {
    //            throw new IllegalArgumentException("provider is not enabled");
    //        }
    //
    //        // authority type is the cp type
    //        ProviderAuthority<? extends ResourceProvider<?>, ?, ? extends ConfigurableProvider, ?, ?> pa = getAuthorityService(
    //                cp.getType()).getAuthority(cp.getAuthority());
    //
    //        // register directly with authority
    //        ResourceProvider<?> p = pa.registerProvider(cp);
    //        return p;
    //    }
    //
    //    public void unregisterResourceProvider(ConfigurableProvider cp)
    //            throws NoSuchAuthorityException, NoSuchProviderException {
    //        if (cp == null) {
    //            throw new IllegalArgumentException("invalid provider");
    //        }
    //
    //        // authority type is the cp type
    //        ProviderAuthority<? extends ResourceProvider<?>, ?, ? extends ConfigurableProvider, ?, ?> pa = getAuthorityService(
    //                cp.getType()).getAuthority(cp.getAuthority());
    //
    //        pa.unregisterProvider(cp.getProvider());
    //    }

}
