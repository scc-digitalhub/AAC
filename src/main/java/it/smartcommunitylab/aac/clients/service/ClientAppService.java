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

package it.smartcommunitylab.aac.clients.service;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.model.ClientApp;
import java.util.Collection;

/*
 * Client App serves UI and API
 */
public interface ClientAppService {
    /*
     * Client registration, per realm
     */
    public Collection<ClientApp> listClients(String realm);

    public ClientApp findClient(String clientId);

    public ClientApp getClient(String clientId) throws NoSuchClientException;

    public ClientApp updateClient(String clientId, ClientApp app) throws NoSuchClientException;

    public ClientApp registerClient(String realm, String name);

    public ClientApp registerClient(String realm, ClientApp app);

    public void deleteClient(String clientId);

    /*
     * Configuration schema
     *
     * TODO move to configurableProperties, which contains a schema
     */
    public JsonSchema getConfigurationSchema();
}
