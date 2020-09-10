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

package it.smartcommunitylab.aac.oauth;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import it.smartcommunitylab.aac.jwt.ClientKeyCacheService;
import it.smartcommunitylab.aac.jwt.JWTEncryptionAndDecryptionService;
import it.smartcommunitylab.aac.jwt.JWTSigningAndValidationService;
import it.smartcommunitylab.aac.jwt.SymmetricKeyJWTValidatorCacheService;
import it.smartcommunitylab.aac.manager.ClaimManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * Implementation of the {@link OAuthFlowExtensions} with the Web hook functionality. 
 * The call is accompanied with the JWT token of the requesting client with respect to the authorized user scope claims.
 * @author raman
 *
 */
public class WebhookOAuthFlowExtensions implements OAuthFlowExtensions {

	private static final Logger logger = LoggerFactory.getLogger(WebhookOAuthFlowExtensions.class);
	
	@Value("${jwt.issuer}")
	private String issuer;
    
	@Autowired
    private RoleManager roleManager;
    @Autowired
    private UserManager userManager;        
	@Autowired
	private ClaimManager claimManager;
	@Autowired
	ClientDetailsRepository clientRepo;
	@Autowired
	private ClientKeyCacheService encrypters;
	@Autowired
	private JWTSigningAndValidationService jwtService;
	@Autowired
	private SymmetricKeyJWTValidatorCacheService symmetricCacheService;
	@Autowired
    private ServiceManager serviceManager;
	
	private RestTemplate restTemplate;
	
	@Value("${hook.timeout:10000}")
	private int timeout;
	
	@PostConstruct
	public void init() {
	    RequestConfig config = RequestConfig.custom()
	      .setConnectTimeout(timeout)
	      .setConnectionRequestTimeout(timeout)
	      .setSocketTimeout(timeout)
	      .build();
	    CloseableHttpClient client = HttpClientBuilder
	      .create()
	      .setDefaultRequestConfig(config)
	      .build();
		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(client);
		restTemplate = new RestTemplate(clientHttpRequestFactory);
	}
	

	@Override
	public void onAfterApproval(AuthorizationRequest authorizationRequest, Authentication userAuthentication) throws FlowExecutionException {
		ClientDetailsEntity client = clientRepo.findByClientId(authorizationRequest.getClientId());
		if(client == null) {
		    logger.error("invalid cliendId: " + String.valueOf(authorizationRequest.getClientId()));
		    throw new FlowExecutionException("Invalid cliendId: " + String.valueOf(authorizationRequest.getClientId()));
		}
		
		Map<String, Object> additional = client.getAdditionalInformation();
		String hook = (String) additional.get("onAfterApprovalWebhook");
		if (StringUtils.isNotBlank(hook)) {			
			try {
				URL url = new URL(hook);
				String token = extractToken(authorizationRequest, userAuthentication, client);
				HttpHeaders headers = new HttpHeaders();
				headers.set("Authorization", "Bearer " + token);
				HttpEntity<Void> entity = new HttpEntity<>(headers);

				ResponseEntity<String> result = restTemplate.exchange(url.toString(), HttpMethod.GET, entity, String.class);				
				logger.debug("Hook response code: " + result.getStatusCodeValue());
                logger.debug("Hook result: " + result.getBody());				
			} catch (MalformedURLException e) {
				throw new FlowExecutionException("Invalid hook URL: " + hook);
			} catch (Exception e) {
				throw new FlowExecutionException("Hook invocation failure: " + e.getMessage());
			}
		}
	}
	
	protected String extractToken(AuthorizationRequest authorizationRequest, Authentication userAuthentication, ClientDetailsEntity client) throws FlowExecutionException {
	    //TODO cleanup handling of autorities in session!! it's a mess
        // note: session with principal contains stale info about roles, fetched at
        // login
        Collection<? extends GrantedAuthority> selectedAuthorities = userAuthentication
                .getAuthorities();
        // fetch again from db
        try {
            User user = (User) userAuthentication.getPrincipal();
            long userId = Long.parseLong(user.getUsername());
            it.smartcommunitylab.aac.model.User userEntity = userManager.findOne(userId);
            selectedAuthorities = roleManager.buildAuthorities(userEntity);
        } catch (Exception e) {
            // user is not available
            logger.error("user not found: " + e.getMessage());
        }   
	    
		Map<String, Object> claimMap = claimManager.getUserClaims(userAuthentication.getName(), selectedAuthorities, client, authorizationRequest.getScope(), null, null);
		JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
		claimMap.entrySet().forEach(e -> builder.claim(e.getKey(), e.getValue()));
		
        // eventual client claims
        Map<String, Object> clientClaims = claimManager.getClientClaims(client.getClientId(), authorizationRequest.getScope());
        clientClaims.entrySet().forEach(e -> builder.claim(e.getKey(), e.getValue()));

		
        List<String> audiences = new LinkedList<>();
        audiences.add(client.getClientId());
        audiences.addAll(getServiceIds(authorizationRequest.getScope()));

		JWTClaimsSet claims = builder
				.audience(audiences)
				.claim("azp", client.getClientId())
				.issuer(issuer)
				.issueTime(new Date())
				.jwtID(UUID.randomUUID().toString()) // set a random NONCE in the middle of it
				.build();

		JWSAlgorithm signedResponseAlg = ClientKeyCacheService.getSignedResponseAlg(client);
		JWEAlgorithm encResponseAlg = ClientKeyCacheService.getEncryptedResponseAlg(client);
		EncryptionMethod encResponseEnc = ClientKeyCacheService.getEncryptedResponseEnc(client);
		String jwksUri = ClientKeyCacheService.getJwksUri(client);
		JWKSet jwks = ClientKeyCacheService.getJwks(client);

		if (encResponseAlg != null && !encResponseAlg.equals(Algorithm.NONE)
				&& encResponseEnc != null && !encResponseEnc.equals(Algorithm.NONE)
				&& (!Strings.isNullOrEmpty(jwksUri) || jwks != null)) {

			// encrypt it to the client's key

			JWTEncryptionAndDecryptionService encrypter = encrypters.getEncrypter(client);

			if (encrypter != null) {

				EncryptedJWT encrypted = new EncryptedJWT(new JWEHeader(encResponseAlg, encResponseEnc), claims);

				encrypter.encryptJwt(encrypted);

				return encrypted.serialize();
			} else {
				logger.error("Couldn't find encrypter for client: " + client.getClientId());
				throw new FlowExecutionException("Couldn't find encrypter for client: " + client.getClientId());
			}
		} else {

			JWSAlgorithm signingAlg = jwtService.getDefaultSigningAlgorithm(); // default to the server's preference
			if (signedResponseAlg != null) {
				signingAlg = signedResponseAlg; // override with the client's preference if available
			}
			JWSHeader header = new JWSHeader(signingAlg, null, null, null, null, null, null, null, null, null,
					jwtService.getDefaultSignerKeyId(),
					null, null);
			SignedJWT signed = new SignedJWT(header, claims);

			if (signingAlg.equals(JWSAlgorithm.HS256)
					|| signingAlg.equals(JWSAlgorithm.HS384)
					|| signingAlg.equals(JWSAlgorithm.HS512)) {

				// sign it with the client's secret
				JWTSigningAndValidationService signer = symmetricCacheService.getSymmetricValidator(client);
				signer.signJwt(signed);

			} else {
				// sign it with the server's key
				jwtService.signJwt(signed);
			}

			return signed.serialize();
		}
	}

    private Set<String> getServiceIds(Set<String> scopes) {
    	if (scopes != null && !scopes.isEmpty()) {
    		return serviceManager.findServiceIdsByScopes(scopes);
    	}
    	return Collections.emptySet();
    }

}
