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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.net.URLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

/*
 * IconStore is a utility class used to recover the icon associated to a
 * SPID identity provider. Icon URLs are obtained from a request with a request
 * with pattern
 *  https://registry.spid.gov.it/entities-idp/{entityId}?output=json
 */
public class IconStore {

    private static final String DEFAULT_BASE_URL = "https://registry.spid.gov.it/";
    private static final Logger logger = LoggerFactory.getLogger(IconStore.class);
    private final String baseUrl;

    public IconStore() {
        this(DEFAULT_BASE_URL);
    }

    public IconStore(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private String buildMetadataUri(String entityId) throws MalformedURLException {
        URLBuilder builder = new URLBuilder(baseUrl);
        builder.setPath("entities-idp/" + URLEncoder.encode(entityId, Charset.defaultCharset()));
        builder.getQueryParams().add(new Pair<>("output", "json"));
        return builder.buildURL();
    }

    public @Nullable String GetIcon(String entityId) {
        // fetch metadata
        URL metadataUrl;
        try {
            String metadataLocation = buildMetadataUri(entityId);
            metadataUrl = new URL(metadataLocation);
        } catch (MalformedURLException e) {
            logger.error(
                "unable to build request uri for icon for entity id " + entityId + " due to error: " + e.getMessage()
            );
            return null;
        }
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) metadataUrl.openConnection();
            conn.setRequestMethod("GET");
        } catch (IOException e) {
            logger.error(
                "unable to open connection to retrieve metadata for entity id " +
                entityId +
                " due to error: " +
                e.getMessage()
            );
            return null;
        }
        StringBuilder metadataResult = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null;) {
                metadataResult.append(line);
            }
        } catch (IOException e) {
            conn.disconnect();
            logger.error("failed to request metadata for entity id " + entityId + " due to error: " + e.getMessage());
            return null;
        }
        conn.disconnect();
        // parse result
        SpidIdpMetadataModel model;
        try {
            model = new ObjectMapper().readValue(metadataResult.toString(), SpidIdpMetadataModel.class);
        } catch (JsonProcessingException e) {
            logger.error("failed to parse metadata for entity id " + entityId + " due to error: " + e.getMessage());
            return null;
        }
        return model.getLogoUri();
    }
}
