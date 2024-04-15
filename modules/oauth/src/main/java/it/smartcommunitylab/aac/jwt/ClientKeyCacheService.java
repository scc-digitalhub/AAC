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

package it.smartcommunitylab.aac.jwt;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.util.Base64URL;
import it.smartcommunitylab.aac.jose.JWKSetKeyStore;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 *
 * Takes in a client and returns the appropriate validator or encrypter for that
 * client's registered key types.
 *
 * @author jricher
 * @author mat
 *
 */

public class ClientKeyCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ClientKeyCacheService.class);

    private final JWKSetCacheService jwksUriCache = new JWKSetCacheService();

    // cache of validators for by-value JWKs
    private final LoadingCache<JWKSet, JWTSigningAndValidationService> jwksValidators = CacheBuilder
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
        .maximumSize(100)
        .build(new JWKSetVerifierBuilder());

    // cache of encryptors for by-value JWKs
    private final LoadingCache<JWKSet, JWTEncryptionAndDecryptionService> jwksEncrypters = CacheBuilder
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
        .maximumSize(100)
        .build(new JWKSetEncryptorBuilder());

    /*
     * Get client specific signer
     */

    public JWTSigningAndValidationService getSigner(
        String algorithm,
        String clientId,
        String clientSecret,
        String jwks,
        String jwksUri
    ) {
        try {
            JWSAlgorithm alg = JWSAlgorithm.parse(algorithm);

            // fetch client keys if available
            //            String jwks = getJwks(client);
            //            String jwksUri = getJwksUri(client);
            JWKSet set = null;

            // check per algo
            if (
                alg.equals(JWSAlgorithm.RS256) ||
                alg.equals(JWSAlgorithm.RS384) ||
                alg.equals(JWSAlgorithm.RS512) ||
                alg.equals(JWSAlgorithm.ES256) ||
                alg.equals(JWSAlgorithm.ES384) ||
                alg.equals(JWSAlgorithm.ES512) ||
                alg.equals(JWSAlgorithm.PS256) ||
                alg.equals(JWSAlgorithm.PS384) ||
                alg.equals(JWSAlgorithm.PS512)
            ) {
                // asymmetric key from configuration or uri
                if (StringUtils.hasText(jwks)) {
                    set = parseJwks(jwks);
                }
                if (set == null && StringUtils.hasText(jwksUri)) {
                    // try URI
                    set = jwksUriCache.getJWKSet(jwksUri);
                }
            } else if (
                alg.equals(JWSAlgorithm.HS256) || alg.equals(JWSAlgorithm.HS384) || alg.equals(JWSAlgorithm.HS512)
            ) {
                // symmetric key
                // from configuration or uri or secret
                if (StringUtils.hasText(jwks)) {
                    set = parseJwks(jwks);
                }
                if (set == null && StringUtils.hasText(jwksUri)) {
                    // try URI
                    set = jwksUriCache.getJWKSet(jwksUri);
                }
                // we can build a key from secret
                // key length enables us to support only HS256
                if (set == null && alg.equals(JWSAlgorithm.HS256)) {
                    // build keySet from secret
                    JWK jwk = new OctetSequenceKey.Builder(Base64URL.encode(clientSecret))
                        .keyUse(KeyUse.SIGNATURE)
                        .keyID(clientId)
                        .algorithm(alg)
                        .build();

                    set = new JWKSet(jwk);
                }
            } else {
                // unsupported algo, reset
                set = null;
            }

            if (set == null) {
                logger.error("no jwkset found for client");
                return null;
            }

            // check that JWKS contains a key for selected algo
            logger.trace("jwks for " + alg.getName() + ": " + set.getKeys().toString());

            List<JWK> algJwks = set
                .getKeys()
                .stream()
                .filter(k -> (k.getKeyUse() == null || k.getKeyUse().equals(KeyUse.SIGNATURE)))
                .filter(k -> (k.getAlgorithm() != null && k.getAlgorithm().equals(alg)))
                .collect(Collectors.toList());
            if (algJwks.isEmpty()) {
                logger.error("No key for the selected algorithm " + alg.getName());
                return null;
            }

            logger.trace("jwks selected for " + alg.getName() + ": " + algJwks.toString());

            // fetch from cache via loader, passing only matching keys
            return jwksValidators.get(new JWKSet(algJwks));
        } catch (UncheckedExecutionException | ExecutionException | IllegalArgumentException e) {
            logger.error("Problem loading client validator", e);
            return null;
        }
    }

    /*
     * Get client specific encrypter
     */

    public JWTEncryptionAndDecryptionService getEncrypter(
        String algorithm,
        String clientId,
        String clientSecret,
        String jwks,
        String jwksUri
    ) {
        try {
            JWEAlgorithm alg = JWEAlgorithm.parse(algorithm);

            // fetch client keys if available
            //            String jwks = getJwks(client);
            //            String jwksUri = getJwksUri(client);
            JWKSet set = null;

            // asymmetric key from configuration or uri
            if (StringUtils.hasText(jwks)) {
                set = parseJwks(jwks);
            }
            if (set == null && StringUtils.hasText(jwksUri)) {
                // try URI
                set = jwksUriCache.getJWKSet(jwksUri);
            }

            // TODO add clientSecret as key for AES128+HS256

            if (set == null) {
                logger.error("no jwkset found for client");
                return null;
            }

            // check that JWKS contains a key for selected algo
            logger.trace("jwks for " + alg.getName() + ": " + set.getKeys().toString());

            // TODO rewrite, alg name doesn't directly match key algo
            //            List<JWK> algJwks = set.getKeys().stream().filter(k -> k.getAlgorithm().equals(alg))
            //                    .collect(Collectors.toList());
            //            if (algJwks.isEmpty()) {
            //                logger.error("No key for the selected algorithm " + alg.getName());
            //                return null;
            //            }
            //            logger.trace("jwks selected for " + alg.getName() + ": " + algJwks.toString());

            return jwksEncrypters.get(set);
        } catch (UncheckedExecutionException | ExecutionException e) {
            logger.error("Problem loading client encrypter", e);
            return null;
        }
    }

    private JWKSet parseJwks(String data) {
        if (data != null) {
            try {
                JWKSet jwks = JWKSet.parse(data);
                logger.trace("parsed jwks is " + jwks.getKeys().toString());
                return jwks;
            } catch (ParseException e) {
                logger.error("Unable to parse JWK Set", e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    /*
     * Cache loaders with single key used as default
     */

    private class JWKSetEncryptorBuilder extends CacheLoader<JWKSet, JWTEncryptionAndDecryptionService> {

        @Override
        public JWTEncryptionAndDecryptionService load(JWKSet key) throws Exception {
            return new DefaultJWTEncryptionAndDecryptionService(new JWKSetKeyStore(key));
        }
    }

    private class JWKSetVerifierBuilder extends CacheLoader<JWKSet, JWTSigningAndValidationService> {

        @Override
        public JWTSigningAndValidationService load(JWKSet key) throws Exception {
            return new DefaultJWTSigningAndValidationService(new JWKSetKeyStore(key));
        }
    }
}
