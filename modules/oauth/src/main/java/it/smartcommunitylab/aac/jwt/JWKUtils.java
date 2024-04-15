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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JWKUtils {

    private static final Logger logger = LoggerFactory.getLogger(JWKUtils.class);

    public static JWK generateRsaJWK(String id, String usage, String alg, int length)
        throws IllegalArgumentException, JOSEException {
        logger.debug(
            "generate RSA jwk for " +
            id +
            " use " +
            usage +
            " with length " +
            String.valueOf(length) +
            "  with algorithm " +
            alg
        );

        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }

        // validate keyUse
        KeyUse use = new KeyUse(usage);

        // validate algorithm
        JWSAlgorithm algo = JWSAlgorithm.parse(alg);

        return new RSAKeyGenerator(length).keyUse(use).keyID(id).algorithm(algo).generate();
    }

    public static JWK generateECJWK(String id, String usage, String alg, String curve)
        throws IllegalArgumentException, JOSEException {
        logger.debug(
            "generate EC jwk for " + id + " use " + usage + " with curve " + curve + "  with algorithm " + alg
        );

        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }

        // validate keyUse
        KeyUse use = new KeyUse(usage);

        // validate curve
        Curve ecurve = Curve.parse(curve);

        // validate algorithm
        JWSAlgorithm algo = JWSAlgorithm.parse(alg);

        return new ECKeyGenerator(ecurve).keyUse(use).keyID(id).algorithm(algo).generate();
    }

    public static JWK generateHMACJWT(String id, String usage, String alg, int length)
        throws IllegalArgumentException, JOSEException {
        logger.debug("generate HMAC jwk for " + id + " use " + usage + "  with algorithm " + alg);

        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }

        // validate keyUse
        KeyUse use = new KeyUse(usage);

        // validate algorithm
        JWSAlgorithm algo = JWSAlgorithm.parse(alg);

        return new OctetSequenceKeyGenerator(length).keyID(id).keyUse(use).algorithm(algo).generate();
    }
}
