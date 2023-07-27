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

import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.base.BaseClient;
import it.smartcommunitylab.aac.core.model.ClientCredentials;
import java.util.Collection;

/*
 * Client services
 *
 *
 */
public interface ClientService {
    /*
     * Client registration
     */
    public BaseClient getClient(String clientId) throws NoSuchClientException;

    //    public Collection<BaseClient> listClients();

    /*
     * Client credentials
     */

    public Collection<ClientCredentials> getClientCredentials(String clientId) throws NoSuchClientException;

    public ClientCredentials getClientCredentials(String clientId, String credentialsId) throws NoSuchClientException;

    public ClientCredentials resetClientCredentials(String clientId, String credentialsId) throws NoSuchClientException;

    public ClientCredentials setClientCredentials(String clientId, String credentialsId, ClientCredentials credentials)
        throws NoSuchClientException;

    public void removeClientCredentials(String clientId, String credentialsId) throws NoSuchClientException;
}
