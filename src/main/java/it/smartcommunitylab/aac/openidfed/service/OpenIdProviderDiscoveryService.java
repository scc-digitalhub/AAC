// Copyright 2023 the original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package it.smartcommunitylab.aac.openidfed.service;

import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import org.springframework.lang.Nullable;

import java.util.Collection;

public interface OpenIdProviderDiscoveryService {
    public Collection<String> discoverProviders();

    public @Nullable OIDCProviderMetadata findProvider(String entityId);

    public @Nullable FederationEntityMetadata loadProviderMetadata(String entityId);
}
