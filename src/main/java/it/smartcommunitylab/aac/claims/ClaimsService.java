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

package it.smartcommunitylab.aac.claims;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.users.model.UserDetails;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public interface ClaimsService {
    /*
     * Complete mapping TODO move to dedicated interface claimMapper
     */
    public Map<String, Serializable> getUserClaims(
        UserDetails user,
        String realm,
        ClientDetails client,
        Collection<String> scopes,
        Collection<String> resourceIds,
        Map<String, Serializable> extensions
    ) throws NoSuchResourceException, InvalidDefinitionException, SystemException;

    public Map<String, Serializable> getClientClaims(
        ClientDetails client,
        Collection<String> scopes,
        Collection<String> resourceIds,
        Map<String, Serializable> extensions
    ) throws NoSuchResourceException, InvalidDefinitionException, SystemException;
    //    /*
    //     * ProfileMapping
    //     */
    //
    //    public Map<String, Serializable> getUserClaimsFromBasicProfile(UserDetails user);
    //
    //    public Map<String, Serializable> getUserClaimsFromOpenIdProfile(UserDetails user, Collection<String> scopes);
    //
    //    /*
    //     * Service mapping
    //     */
    //    public Map<String, Serializable> getUserClaimsFromResource(UserDetails user, ClientDetails client,
    //            Collection<String> scopes,
    //            String resourceId) throws NoSuchResourceException;
    //
    //    public Map<String, Serializable> getClientClaimsFromResource(ClientDetails client, Collection<String> scopes,
    //            String resourceId) throws NoSuchResourceException;

    //    /*
    //     * Function mapping for hooks
    //     */
    //    public Map<String, Serializable> mapUserClaimsWithFunction(UserDetails user, ClientDetails client,
    //            Collection<String> scopes,
    //            String functionName, String functionCode) throws InvalidDefinitionException;
    //
    //    public Map<String, Serializable> mapClientClaimsFromFunction(ClientDetails client, Collection<String> scopes,
    //            String functionName, String functionCode) throws InvalidDefinitionException;

}
