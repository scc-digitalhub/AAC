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

package it.smartcommunitylab.aac.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import it.smartcommunitylab.aac.jose.JWKSetKeyStore;
import it.smartcommunitylab.aac.jwt.JWKUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;

/*
 * Keystore is @2, we need this to build security services and config
 */

@Configuration
@Order(3)
public class KeyStoreConfig {

    @Value("${security.keystore}")
    private Resource location;

    private JWKSetKeyStore keyStore;

    @Bean
    @Primary
    public JWKSetKeyStore getJWKSetKeyStore() {
        if (keyStore == null) {
            if (location.exists()) {
                // load from resource
                keyStore = load(location);
                // check if empty
                if (keyStore.getKeys().isEmpty()) {
                    // discard, we will generate a new one
                    keyStore = null;
                }
            }
        }

        if (keyStore == null) {
            // generate new in-memory keystore
            keyStore = generate();
        }

        return keyStore;
    }

    private JWKSetKeyStore load(Resource location) {
        JWKSetKeyStore keystore = new JWKSetKeyStore();
        keystore.setLocation(location);

        return keystore;
    }

    private JWKSetKeyStore generate() {
        JWKSetKeyStore keystore = new JWKSetKeyStore();

        try {
            // build a default RSA2048 key for sign
            JWK jwk = JWKUtils.generateRsaJWK("rsa1", "sig", "RS256", 2048);
            JWKSet jwks = new JWKSet(jwk);
            keystore.setJwkSet(jwks);
        } catch (IllegalArgumentException | JOSEException e) {
            // ignore, will return an empty keystore
        }

        return keystore;
    }
}
