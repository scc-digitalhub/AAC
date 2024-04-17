/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import it.smartcommunitylab.aac.spid.service.SpidRegistry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

/*
 * https://registry.spid.gov.it/assets/data/idp.json
 */
public class RemoteSpidRegistry implements SpidRegistry {

    private static final int CONNECTION_TIMEOUT = 100000;
    private static final Logger logger = LoggerFactory.getLogger(RemoteSpidRegistry.class);
    private static final String DEFAULT_REGISTRY_URL = "https://registry.spid.gov.it/assets/data/idp.json";
    private final String registryUrl;
    private Map<String, SpidRegistration> cachedRegs; // TODO: actual cache?

    public RemoteSpidRegistry() {
        this(DEFAULT_REGISTRY_URL);
    }

    public RemoteSpidRegistry(String registryUrl) {
        this.registryUrl = registryUrl;
        this.cachedRegs = initRegistry(); // TODO: disaccoppiare responsabilità: questo non è buon codice
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    private @Nullable Map<String, SpidRegistration> initRegistry() {
        // TODO: disaccoppiare reponsabilità perché questa classe fa multiple cose.
        String registry;
        try {
            registry = loadRemoteRegistry(registryUrl);
        } catch (MalformedURLException e) {
            logger.error("unable to load remote registry due to malformed url: " + e.getMessage());
            return null;
        } catch (IOException e) {
            logger.error("unable to load remote registry due connection error: " + e.getMessage());
            return null;
        }
        SpidDataModel model;
        try {
            model = new ObjectMapper().readValue(registry, SpidDataModel.class);
        } catch (JsonProcessingException e) {
            logger.error("unable to parse remote registry as json: " + e.getMessage());
            return null;
        }
        List<SpidRegistration> parsedRegistrations = ModelToRegistrationConverter.Convert(model);
        return parsedRegistrations.stream().collect(Collectors.toMap(SpidRegistration::getEntityId, e -> e));
    }

    @Override
    public Collection<SpidRegistration> getIdentityProviders() {
        if (cachedRegs != null) {
            return cachedRegs.values();
        }
        return null;
    }

    @Override
    public SpidRegistration getIdentityProvider(String entityId) {
        if (cachedRegs != null) {
            return cachedRegs.get(entityId);
        }
        return null;
    }

    private String loadRemoteRegistry(String registryUrl) throws MalformedURLException, IOException {
        URL registryLocation = new URL(registryUrl);
        HttpURLConnection con = (HttpURLConnection) registryLocation.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setConnectTimeout(CONNECTION_TIMEOUT);
        con.setReadTimeout(CONNECTION_TIMEOUT);
        con.setInstanceFollowRedirects(false);
        BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = inputBuffer.readLine()) != null) {
            content.append(line);
        }
        inputBuffer.close();
        con.disconnect();
        return content.toString();
    }
}
