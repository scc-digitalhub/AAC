/**
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

package it.smartcommunitylab.aac.openidfed.resolvers;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import java.util.List;
import org.springframework.lang.Nullable;

/*
 * Federation entity operations
 */
public interface FederationEntityResolver {
    public FederationEntityMetadata resolveFederationEntityMetadata(String trustAnchor, String entityId)
        throws ResolveException;

    public List<EntityID> listFederationEntities(String trustAnchor, String entityId, @Nullable EntityType type)
        throws ResolveException;
    //TODO
    //fetch
    //resolve
    //trust_mark_status
    //trust_mark_list
    //historical_keys

}
