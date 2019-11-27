/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.config;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;

import it.smartcommunitylab.aac.jose.JWKSetKeyStore;
import it.smartcommunitylab.aac.jwt.DefaultJWTEncryptionAndDecryptionService;
import it.smartcommunitylab.aac.jwt.DefaultJWTSigningAndValidationService;
import it.smartcommunitylab.aac.jwt.JWTEncryptionAndDecryptionService;
import it.smartcommunitylab.aac.jwt.JWTSigningAndValidationService;

/**
 * @author raman
 *
 */
@Configuration
public class JWTConfig {

    @Value("${jwt.kid.sig}")
    private String sigKid;

    @Value("${jwt.kid.enc}")
    private String encKid;

    @Autowired
    private JWKSetKeyStore jwtKeyStore;

    @Bean()
    public JWTSigningAndValidationService getJWTSigningAndValidationService()
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        // always check key
        JWK key = null;

        if (sigKid.isEmpty()) {
            // fetch first with sig usage
            for (JWK jwk : jwtKeyStore.getKeys()) {
                if (jwk.getKeyUse().equals(KeyUse.SIGNATURE)) {
                    key = jwk;
                    break;
                }
            }
        } else {
            // check match
            for (JWK jwk : jwtKeyStore.getKeys()) {
                if (jwk.getKeyID().equals(sigKid)
                        && jwk.getKeyUse().equals(KeyUse.SIGNATURE)) {
                    key = jwk;
                    break;
                }
            }
        }

        if (key == null) {
            throw new InvalidKeySpecException();
        }

        DefaultJWTSigningAndValidationService service = new DefaultJWTSigningAndValidationService(jwtKeyStore);
        service.setDefaultSignerKeyId(key.getKeyID());
        // optional, set default algorithm
        // rarely used since all operations with default signer
        // will use the default key as id
        if (key.getAlgorithm() != null) {
            service.setDefaultSigningAlgorithmName(key.getAlgorithm().getName());
        }
        return service;
    }

    @Bean()
    public JWTEncryptionAndDecryptionService getJWTEncryptionAndDecryptionService()
            throws JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {

        // check key if required
        JWK key = null;

        if (!encKid.isEmpty()) {
            // check match
            for (JWK jwk : jwtKeyStore.getKeys()) {
                if (jwk.getKeyID().equals(encKid)
                        && jwk.getKeyUse().equals(KeyUse.ENCRYPTION) && jwk.isPrivate()) {
                    key = jwk;
                    break;
                }
            }
        }

        DefaultJWTEncryptionAndDecryptionService service = new DefaultJWTEncryptionAndDecryptionService(jwtKeyStore);
        // set default if provided
        if (key != null) {
            service.setDefaultDecryptionKeyId(key.getKeyID());
            service.setDefaultEncryptionKeyId(key.getKeyID());
            // optional, set default algorithm
            // rarely used since all operations with default signer
            // will use the default key as id
            if (key.getAlgorithm() != null) {
                service.setDefaultAlgorithm((JWEAlgorithm) key.getAlgorithm());
            }

        }

        return service;
    }

}
