/*******************************************************************************
 * Copyright 2018 The MIT Internet Trust Consortium
 *
 * Portions copyright 2011-2013 The MITRE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package it.smartcommunitylab.aac.jwt;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.nimbusds.jose.jwk.JWKSet;
import java.text.ParseException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * Creates a caching map of JOSE signers/validators and encrypters/decryptors
 * keyed on the JWK Set URI. Dynamically loads JWK Sets to create the services.
 *
 * @author jricher
 * @author mat
 *
 */

public class JWKSetCacheService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // loading cache for external url
    private final LoadingCache<String, JWKSet> jwksets = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
        .maximumSize(100)
        .build(new JWKSetFetcher(HttpClientBuilder.create().useSystemProperties().build()));

    public JWKSet getJWKSet(String jwksUri) {
        if (!StringUtils.hasText(jwksUri)) {
            return null;
        }

        try {
            return jwksets.get(jwksUri);
        } catch (UncheckedExecutionException | ExecutionException e) {
            logger.warn("Couldn't load JWK Set from " + jwksUri + ": " + e.getMessage());
            return null;
        }
    }

    private class JWKSetFetcher extends CacheLoader<String, JWKSet> {

        private HttpComponentsClientHttpRequestFactory httpFactory;
        private RestTemplate restTemplate;

        JWKSetFetcher(HttpClient httpClient) {
            this.httpFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
            this.restTemplate = new RestTemplate(httpFactory);
        }

        /**
         * Load the JWK Set from URI
         */
        @Override
        public JWKSet load(String uri) throws Exception {
            try {
                String jsonString = restTemplate.getForObject(uri, String.class);
                return JWKSet.parse(jsonString);
            } catch (ParseException | RestClientException e) {
                throw new IllegalArgumentException("Unable to load JWK Set");
            }
        }
    }
}
