package it.smartcommunitylab.aac.jwt;

import java.text.ParseException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.jose.JWKSetKeyStore;
import it.smartcommunitylab.aac.jwt.JWTEncryptionAndDecryptionService;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;

/**
 *
 * Takes in a client and returns the appropriate validator or encrypter for
 * that client's registered key types.
 *
 * @author jricher
 *
 */
@Service
public class ClientKeyCacheService {
    private static final Logger logger = LoggerFactory.getLogger(ClientKeyCacheService.class);


	@Autowired
	private JWKSetCacheService jwksUriCache = new JWKSetCacheService();

	@Autowired
	private SymmetricKeyJWTValidatorCacheService symmetricCache = new SymmetricKeyJWTValidatorCacheService();

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

    public JWTSigningAndValidationService getSigner(ClientDetailsEntity client) {
        JWSAlgorithm signingAlg = getSignedResponseAlg(client);
        if (signingAlg != null) {
            return getSigner(client, signingAlg);
        } else {
            return null;
        }
    }
	
	
	private JWTSigningAndValidationService getSigner(ClientDetailsEntity client, JWSAlgorithm alg) {

		try {
			if (alg.equals(JWSAlgorithm.RS256)
					|| alg.equals(JWSAlgorithm.RS384)
					|| alg.equals(JWSAlgorithm.RS512)
					|| alg.equals(JWSAlgorithm.ES256)
					|| alg.equals(JWSAlgorithm.ES384)
					|| alg.equals(JWSAlgorithm.ES512)
					|| alg.equals(JWSAlgorithm.PS256)
					|| alg.equals(JWSAlgorithm.PS384)
					|| alg.equals(JWSAlgorithm.PS512)) {

				// asymmetric key from configuration
				JWKSet set = getJwks(client);
				if (set != null) {
					return jwksValidators.get(set);
				} else if (!Strings.isNullOrEmpty(getJwksUri(client))) {
				    //load from external uri
					return jwksUriCache.getValidator(getJwksUri(client));
				} else {
					return null;
				}

			} else if (alg.equals(JWSAlgorithm.HS256)
					|| alg.equals(JWSAlgorithm.HS384)
					|| alg.equals(JWSAlgorithm.HS512)) {

				// symmetric key
				return symmetricCache.getSymmetricValidator(client);

			} else {

				return null;
			}
		} catch (UncheckedExecutionException | ExecutionException e) {
			logger.error("Problem loading client validator", e);
			return null;
		}

	}

	public JWTEncryptionAndDecryptionService getEncrypter(ClientDetailsEntity client) {

		try {
            // asymmetric key from configuration
			JWKSet set = getJwks(client);
			if (set != null) {
				return jwksEncrypters.get(set);
			} else if (!Strings.isNullOrEmpty(getJwksUri(client))) {
				return jwksUriCache.getEncrypter(getJwksUri(client));
			} else {
				return null;
			}
		} catch (UncheckedExecutionException | ExecutionException e) {
			logger.error("Problem loading client encrypter", e);
			return null;
		}

	}

	public static String getJwksUri(ClientDetailsEntity client) {
		return (String) client.getAdditionalInformation().get(Config.CLIENT_PARAM_JWKS_URI);
	}

	public static JWKSet getJwks(ClientDetailsEntity client) {
		String data = (String) client.getAdditionalInformation().get(Config.CLIENT_PARAM_JWKS);
		if (data != null) {
			try {
				JWKSet jwks = JWKSet.parse(data);
				return jwks;
			} catch (ParseException e) {
				logger.error("Unable to parse JWK Set", e);
				return null;
			}
		} else {
			return null;
		}
	}
	
	public static JWSAlgorithm getSignedResponseAlg(ClientDetailsEntity client) {
		String signedResponseAlg = (String)client.getAdditionalInformation().get(Config.CLIENT_PARAM_SIGNED_RESPONSE_ALG);
		return signedResponseAlg != null ? JWSAlgorithm.parse(signedResponseAlg) : null;

	}
	public static JWEAlgorithm getEncryptedResponseAlg(ClientDetailsEntity client) {
		String encResponseAlg = (String)client.getAdditionalInformation().get(Config.CLIENT_PARAM_ENCRYPTED_RESPONSE_ALG);
		return encResponseAlg != null ? JWEAlgorithm.parse(encResponseAlg) : null;		
	}
	
	public static EncryptionMethod getEncryptedResponseEnc(ClientDetailsEntity client) {
		String encResponseEnc = (String)client.getAdditionalInformation().get(Config.CLIENT_PARAM_ENCRYPTED_RESPONSE_ENC);
		return encResponseEnc != null ? EncryptionMethod.parse(encResponseEnc) : null;
	}
	
    public static int getAccessTokenValiditySeconds(ClientDetailsEntity client) {
        Integer validity = client.getAccessTokenValiditySeconds();
        return validity != null ? validity : -1;
    }

    public static int getRefreshTokenValiditySeconds(ClientDetailsEntity client) {
        Integer validity = client.getRefreshTokenValiditySeconds();
        return validity != null ? validity : -1;
    }

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