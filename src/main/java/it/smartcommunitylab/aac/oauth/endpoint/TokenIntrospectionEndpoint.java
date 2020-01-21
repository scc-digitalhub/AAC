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

package it.smartcommunitylab.aac.oauth.endpoint;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.util.JsonParser;
import org.springframework.security.oauth2.common.util.JsonParserFactory;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.dto.AACTokenIntrospection;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * OAuth2.0 Token introspection controller as of RFC7662: https://tools.ietf.org/html/rfc7662.
 * @author raman
 *
 */
@Controller
@Api(tags = { "AAC OAuth 2.0 Token Introspection (IETF RFC7662)" })
public class TokenIntrospectionEndpoint {

    public final static String TOKEN_INTROSPECTION_URL = "/oauth/introspect";
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ResourceServerTokenServices resourceServerTokenServices;
	
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	
	@Autowired
	private TokenStore tokenStore;
	
	@Autowired
	private UserManager userManager;	

	@Value("${jwt.issuer}")
	private String issuer;

	private JsonParser objectMapper = JsonParserFactory.create();

	/*
	 * TODO: evaluate if client_id should match audience, as per security considerations
	 * https://tools.ietf.org/html/rfc7662#section-4
	 */
	
	@ApiOperation(value="Get token metadata")
	@RequestMapping(method = RequestMethod.POST, value = TOKEN_INTROSPECTION_URL)
	public ResponseEntity<AACTokenIntrospection> getTokenInfo(@RequestParam String token) {
		AACTokenIntrospection result = new AACTokenIntrospection();
		
		try {
			OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(token);
			
			OAuth2AccessToken storedToken = tokenStore.getAccessToken(auth);		
			
			String clientId = auth.getOAuth2Request().getClientId();
			
			String userName = null;
			String userId = null;
			boolean applicationToken = false;
			
			if (auth.getPrincipal() instanceof User) {
				User principal = (User)auth.getPrincipal();
				userId = principal.getUsername();
			} else {
				ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
				applicationToken = true;
				userId = ""+client.getDeveloperId();				
			}
			userName = userManager.getUserInternalName(Long.parseLong(userId));
			String localName = userName.substring(0, userName.lastIndexOf('@'));
			String tenant = userName.substring(userName.lastIndexOf('@') + 1);

			result.setUsername(localName);
			result.setClient_id(clientId);
			result.setScope(StringUtils.collectionToDelimitedString(auth.getOAuth2Request().getScope(), " "));
			result.setExp((int)(storedToken.getExpiration().getTime() / 1000));
			result.setIat(result.getExp() - storedToken.getExpiresIn());
			result.setIss(issuer);
			result.setNbf(result.getIat());
			result.setSub(userId);
			result.setAud(new String[] {clientId});

            // check if token is JWT then return jti
            if (isJwt(storedToken.getValue())) {
                //check again expiration status
                long now = new Date().getTime() / 1000;
                long expiration = (storedToken.getExpiration().getTime() / 1000);
                if(expiration < now) {
                    throw new InvalidTokenException("token expired");
                }
                
                // extract tokenId from jti field
                Jwt old = JwtHelper.decode(storedToken.getValue());
                Map<String, Object> claims = this.objectMapper.parseMap(old.getClaims());
                result.setJti((String) claims.get("jti"));
                // replace aud with JWT aud
                result.setAud(((List<String>) claims.get("aud")).toArray(new String[0]));
            }
			
			// only bearer tokens supported
			result.setToken_type(OAuth2AccessToken.BEARER_TYPE);
			
			//if token exists is valid since revoked ones are deleted
			result.setActive(true);

			result.setAac_user_id(userId);
			result.setAac_grantType(auth.getOAuth2Request().getGrantType());
			result.setAac_applicationToken(applicationToken);
			result.setAac_am_tenant(tenant);
		} catch (Exception e) {
			logger.error("Error getting info for token: "+ e.getMessage());
			result = new AACTokenIntrospection();
			result.setActive(false);
		}
		return ResponseEntity.ok(result);
	}
	
    private boolean isJwt(String value) {
        // simply check for format header.body.signature
        int firstPeriod = value.indexOf('.');
        int lastPeriod = value.lastIndexOf('.');

        if (firstPeriod <= 0 || lastPeriod <= firstPeriod) {
            return false;
        } else {
            return true;
        }
    }
	
}
