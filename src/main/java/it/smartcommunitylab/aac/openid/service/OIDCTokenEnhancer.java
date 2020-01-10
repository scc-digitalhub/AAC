
package it.smartcommunitylab.aac.openid.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTClaimsSet.Builder;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.jwt.ClientKeyCacheService;
import it.smartcommunitylab.aac.jwt.JWTEncryptionAndDecryptionService;
import it.smartcommunitylab.aac.jwt.JWTSigningAndValidationService;
import it.smartcommunitylab.aac.jwt.SymmetricKeyJWTValidatorCacheService;
import it.smartcommunitylab.aac.manager.ClaimManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.ResourceRepository;
/**
 * Default implementation of service to create specialty OpenID Connect tokens.
 *
 * @author Amanda Anganes
 *
 */
@Service
public class OIDCTokenEnhancer  {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	public String MAX_AGE = "max_age";
	public String NONCE = "nonce";
	public static final String AUTH_TIMESTAMP = "AUTH_TIMESTAMP";

	@Value("${jwt.issuer}")
	private String issuer;

	@Autowired
	private ClaimManager claimManager;
	
	@Autowired
	private JWTSigningAndValidationService jwtService;

	@Autowired
	private ClientKeyCacheService encrypters;

	@Autowired
	private SymmetricKeyJWTValidatorCacheService symmetricCacheService;
	
	@Autowired
	private ClientDetailsRepository clientRepository;
	
    @Autowired
    private UserManager userManager;

    @Autowired
	private ResourceRepository resourceRepository;

	public JWT createIdToken(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
		OAuth2Request request = authentication.getOAuth2Request();
		String clientId = request.getClientId();
		
		logger.debug("fetch user via authentication");
//		User user = userManager.getUser();
		User user = null;
        try {
            // fetch from auth
            Object principal = authentication.getPrincipal();
            org.springframework.security.core.userdetails.User auth = (org.springframework.security.core.userdetails.User) principal;
            logger.debug("principal username " + auth.getUsername());

            //fetch user from db
            long userId = Long.parseLong(auth.getUsername());
            user = userManager.findOne(userId);
            
        } catch (Exception e) {
           // user is not available, thus all user claims will fail
            logger.error("user not found: "+e.getMessage());
        }
		ClientDetailsEntity client = clientRepository.findByClientId(clientId);
		
//		SignedJWT signed = createJWT(client, accessToken, authentication);
		String signed = accessToken.getValue();
	    logger.trace("signed access token used for oidc is "+signed);

		JWSAlgorithm signingAlg = jwtService.getDefaultSigningAlgorithm();

		if (ClientKeyCacheService.getSignedResponseAlg(client) != null) {
			signingAlg = ClientKeyCacheService.getSignedResponseAlg(client);
		}


		JWT idToken = null;

		JWTClaimsSet.Builder idClaims = new JWTClaimsSet.Builder();
		//add claims for user details if requested via scopes
		if(user != null) {
            Set<String> scope = new HashSet<>(request.getScope());
            if (!scope.contains(Config.SCOPE_OPENID)) {
            	scope.add(Config.SCOPE_OPENID);
            }
            Collection<? extends GrantedAuthority> selectedAuthorities = authentication.getOAuth2Request().getAuthorities();
            Map<String, Object> userClaims = claimManager.createUserClaims(user.getId().toString(), selectedAuthorities, client, scope, null, null);
            // set directly, ignore extracted
            userClaims.remove("sub");
            userClaims.entrySet().forEach(e -> idClaims.claim(e.getKey(), e.getValue()));
        }

		// if the auth time claim was explicitly requested OR if the client always wants the auth time, put it in
		//TODO check "idtoken" vs "id_token"
		if (request.getExtensions().containsKey(MAX_AGE) || (request.getExtensions().containsKey("idtoken"))) {

			if (request.getExtensions().get(AUTH_TIMESTAMP) != null) {

				Long authTimestamp = Long.parseLong((String) request.getExtensions().get(AUTH_TIMESTAMP));
				if (authTimestamp != null) {
					idClaims.claim("auth_time", authTimestamp / 1000L);
				}
			} else {
				// we couldn't find the timestamp!
				logger.warn("Unable to find authentication timestamp! There is likely something wrong with the configuration.");
			}
		}

		idClaims.issueTime(new Date());

		if (accessToken.getExpiration() != null) {
			Date expiration = accessToken.getExpiration();
			idClaims.expirationTime(expiration);
		}

        List<String> audiences = new LinkedList<>();
        audiences.add(clientId);
        audiences.addAll(getServiceIds(request.getScope()));

		idClaims.issuer(issuer);
		idClaims.subject(authentication.getName());
		idClaims.audience(audiences);
		idClaims.jwtID(UUID.randomUUID().toString()); // set a random NONCE in the middle of it
        idClaims.claim("azp", clientId);

		String nonce = (String)request.getExtensions().get(NONCE);
		if (!Strings.isNullOrEmpty(nonce)) {
			idClaims.claim("nonce", nonce);
		}

		//add additional claims for scopes
		idClaims.claim("scope", String.join(" ", request.getScope()));
		
		Set<String> responseTypes = request.getResponseTypes();

		if (responseTypes.contains("token")) {
			// calculate the token hash
			Base64URL at_hash = IdTokenHashUtils.getAccessTokenHash(signingAlg, signed);
			idClaims.claim("at_hash", at_hash);
		}

		if (ClientKeyCacheService.getEncryptedResponseAlg(client) != null && !ClientKeyCacheService.getEncryptedResponseAlg(client).equals(Algorithm.NONE)
				&& ClientKeyCacheService.getEncryptedResponseEnc(client) != null && !ClientKeyCacheService.getEncryptedResponseEnc(client).equals(Algorithm.NONE)
				&& (!Strings.isNullOrEmpty(ClientKeyCacheService.getJwksUri(client)) || ClientKeyCacheService.getJwks(client) != null)) {

			JWTEncryptionAndDecryptionService encrypter = encrypters.getEncrypter(client);

			if (encrypter != null) {

				idToken = new EncryptedJWT(new JWEHeader(ClientKeyCacheService.getEncryptedResponseAlg(client), ClientKeyCacheService.getEncryptedResponseEnc(client)), idClaims.build());

				encrypter.encryptJwt((JWEObject) idToken);

			} else {
				logger.error("Couldn't find encrypter for client: " + client.getClientId());
			}

		} else {

			if (signingAlg.equals(Algorithm.NONE)) {
				// unsigned ID token
				idToken = new PlainJWT(idClaims.build());

			} else {

				// signed ID token

				if (signingAlg.equals(JWSAlgorithm.HS256)
						|| signingAlg.equals(JWSAlgorithm.HS384)
						|| signingAlg.equals(JWSAlgorithm.HS512)) {

					JWSHeader header = new JWSHeader(signingAlg, null, null, null, null, null, null, null, null, null,
							jwtService.getDefaultSignerKeyId(),
							null, null);
					idToken = new SignedJWT(header, idClaims.build());

					JWTSigningAndValidationService signer = symmetricCacheService.getSymmetricValidator(client);

					// sign it with the client's secret
					signer.signJwt((SignedJWT) idToken);
				} else {
					idClaims.claim("kid", jwtService.getDefaultSignerKeyId());

					JWSHeader header = new JWSHeader(signingAlg, null, null, null, null, null, null, null, null, null,
							jwtService.getDefaultSignerKeyId(),
							null, null);

					idToken = new SignedJWT(header, idClaims.build());

					// sign it with the server's key
					jwtService.signJwt((SignedJWT) idToken);
				}
			}

		}

		return idToken;
	}
//
//	private SignedJWT createJWT(ClientDetailsEntity client, OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
//		OAuth2Request originalAuthRequest = authentication.getOAuth2Request();
//
//		String clientId = originalAuthRequest.getClientId();
//
//		Builder builder = new JWTClaimsSet.Builder()
//				.claim("azp", clientId)
//				.issuer(issuer)
//				.issueTime(new Date())
//				.expirationTime(accessToken.getExpiration())
//				.subject(authentication.getName())
//				.jwtID(UUID.randomUUID().toString()); // set a random NONCE in the middle of it
//
//		String audience = (String) originalAuthRequest.getExtensions().get("aud");
//		if (!Strings.isNullOrEmpty(audience)) {
//			builder.audience(Lists.newArrayList(audience));
//		}
//
//		JWTClaimsSet claims = builder.build();
//
//		JWSAlgorithm signingAlg = jwtService.getDefaultSigningAlgorithm();
//		JWSHeader header = new JWSHeader(signingAlg, null, null, null, null, null, null, null, null, null,
//				jwtService.getDefaultSignerKeyId(),
//				null, null);
//		SignedJWT signed = new SignedJWT(header, claims);
//
//		jwtService.signJwt(signed);
//		return signed;
//	}	

    private Set<String> getServiceIds(Set<String> scopes) {
    	if (scopes != null && !scopes.isEmpty()) {
    		return resourceRepository.findServicesByResiurceUris(scopes).stream().map(sd -> sd.getServiceId()).collect(Collectors.toSet());
    	}
    	return Collections.emptySet();
    }


}