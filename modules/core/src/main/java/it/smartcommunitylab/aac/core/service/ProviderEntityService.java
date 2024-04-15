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
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface ProviderEntityService<P extends ProviderEntity> {
    public List<P> listProviders();

    public List<P> listProvidersByAuthority(String authority);

    public List<P> listProvidersByRealm(String realm);

    public List<P> listProvidersByAuthorityAndRealm(String authority, String realm);

    public P findProvider(String providerId);

    public P getProvider(String providerId) throws NoSuchProviderException;

    public P saveProvider(String providerId, P reg, Map<String, Serializable> configuration);

    public void deleteProvider(String providerId);
}
