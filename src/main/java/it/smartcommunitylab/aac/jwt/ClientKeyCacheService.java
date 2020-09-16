package it.smartcommunitylab.aac.jwt;

import java.text.ParseException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.util.Base64URL;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.jose.JWKSetKeyStore;
import it.smartcommunitylab.aac.jwt.JWTEncryptionAndDecryptionService;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;

/**
 *
 * Takes in a client and returns the appropriate validator or encrypter for that
 * client's registered key types.
 *
 * @author jricher
 *
 */
@Service
public class ClientKeyCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ClientKeyCacheService.class);

    @Autowired
    private JWKSetCacheService jwksUriCache = new JWKSetCacheService();

//	@Autowired
//	private SymmetricKeyJWTValidatorCacheService symmetricCache = new SymmetricKeyJWTValidatorCacheService();

    // cache of validators for by-value JWKs
    private LoadingCache<JWKSet, JWTSigningAndValidationService> jwksValidators;

    // cache of encryptors for by-value JWKs
    private LoadingCache<JWKSet, JWTEncryptionAndDecryptionService> jwksEncrypters;

    public ClientKeyCacheService() {
        this.jwksValidators = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
                .maximumSize(100)
                .build(new JWKSetVerifierBuilder());
        this.jwksEncrypters = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
                .maximumSize(100)
                .build(new JWKSetEncryptorBuilder());
    }

    /*
     * Get client specific signer
     */

//    public JWTSigningAndValidationService getSigner(ClientDetailsEntity client) {
//        JWSAlgorithm signingAlg = getSignedResponseAlg(client);
//        if (signingAlg != null) {
//            return getSigner(client, signingAlg);
//        } else {
//            return null;
//        }
//    }

    public JWTSigningAndValidationService getSigner(
            String algorithm,
            String clientId, String clientSecret,
            String jwks, String jwksUri) {
        try {
            JWSAlgorithm alg = JWSAlgorithm.parse(algorithm);

            // fetch client keys if available
//            String jwks = getJwks(client);
//            String jwksUri = getJwksUri(client);
            JWKSet set = null;

            // check per algo
            if (alg.equals(JWSAlgorithm.RS256)
                    || alg.equals(JWSAlgorithm.RS384)
                    || alg.equals(JWSAlgorithm.RS512)
                    || alg.equals(JWSAlgorithm.ES256)
                    || alg.equals(JWSAlgorithm.ES384)
                    || alg.equals(JWSAlgorithm.ES512)
                    || alg.equals(JWSAlgorithm.PS256)
                    || alg.equals(JWSAlgorithm.PS384)
                    || alg.equals(JWSAlgorithm.PS512)) {

                // asymmetric key from configuration or uri
                if (StringUtils.hasText(jwks)) {
                    set = parseJwks(jwks);
                }
                if (set == null && StringUtils.hasText(jwksUri)) {
                    // try URI
                    set = jwksUriCache.getJWKSet(jwksUri);
                }

            } else if (alg.equals(JWSAlgorithm.HS256)
                    || alg.equals(JWSAlgorithm.HS384)
                    || alg.equals(JWSAlgorithm.HS512)) {

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

            List<JWK> algJwks = set.getKeys().stream()
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
            String clientId, String clientSecret,
            String jwks, String jwksUri) {

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

//    // TODO cleanup and integrate with JWTservice
//    private String getJwksUri(ClientDetailsEntity client) {
//        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWKS_URI);
//    }
//
//    private String getJwks(ClientDetailsEntity client) {
//        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWKS);
//    }
//
//    private JWSAlgorithm getSignedResponseAlg(ClientDetailsEntity client) {
//        String signedResponseAlg = (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_SIGN_ALG);
//        return signedResponseAlg != null ? JWSAlgorithm.parse(signedResponseAlg) : null;
//
//    }
//
//    private JWEAlgorithm getEncryptedResponseAlg(ClientDetailsEntity client) {
//        String encResponseAlg = (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_ENC_ALG);
//        return encResponseAlg != null ? JWEAlgorithm.parse(encResponseAlg) : null;
//    }
//
//    private EncryptionMethod getEncryptedResponseEnc(ClientDetailsEntity client) {
//        String encResponseEnc = (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_ENC_METHOD);
//        return encResponseEnc != null ? EncryptionMethod.parse(encResponseEnc) : null;
//    }

    /*
     * Static helpers to access client properties TODO move to util class or make
     * private
     */

//    public static String getJwksUri(ClientDetailsEntity client) {
//        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWKS_URI);
//    }
//
//    public static String getJwks(ClientDetailsEntity client) {
//        return (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWKS);
//    }
//
//    public static JWSAlgorithm getSignedResponseAlg(ClientDetailsEntity client) {
//        String signedResponseAlg = (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_SIGN_ALG);
//        return signedResponseAlg != null ? JWSAlgorithm.parse(signedResponseAlg) : null;
//
//    }
//
//    public static JWEAlgorithm getEncryptedResponseAlg(ClientDetailsEntity client) {
//        String encResponseAlg = (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_ENC_ALG);
//        return encResponseAlg != null ? JWEAlgorithm.parse(encResponseAlg) : null;
//    }
//
//    public static EncryptionMethod getEncryptedResponseEnc(ClientDetailsEntity client) {
//        String encResponseEnc = (String) client.getAdditionalInformation().get(Config.CLIENT_INFO_JWT_ENC_METHOD);
//        return encResponseEnc != null ? EncryptionMethod.parse(encResponseEnc) : null;
//    }

    public static int getAccessTokenValiditySeconds(ClientDetailsEntity client) {
        Integer validity = client.getAccessTokenValiditySeconds();
        return validity != null ? validity : -1;
    }

    public static int getRefreshTokenValiditySeconds(ClientDetailsEntity client) {
        Integer validity = client.getRefreshTokenValiditySeconds();
        return validity != null ? validity : -1;
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