package it.smartcommunitylab.aac.oauth;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.DefaultSecurityContextAccessor;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.SecurityContextAccessor;
import org.springframework.security.oauth2.provider.TokenRequest;

import com.google.common.collect.Sets;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

public class AACOAuth2RequestFactory<userManager> implements OAuth2RequestFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String NONCE = "nonce";
	
	@Autowired
	private ClientDetailsService clientDetailsService;
	
	//TODO remove, if needed leverage clientService
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;	
	
	//TODO remove, if needed leverage userManager
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private UserManager userManager;
	
	protected SecurityContextAccessor securityContextAccessor = new DefaultSecurityContextAccessor();

	protected boolean checkUserScopes = false;

	/**
	 * @param securityContextAccessor the security context accessor to set
	 */
	public void setSecurityContextAccessor(SecurityContextAccessor securityContextAccessor) {
		this.securityContextAccessor = securityContextAccessor;
	}

	/**
	 * Flag to indicate that scopes should be interpreted as valid authorities. No scopes will be granted to a user
	 * unless they are permitted as a granted authority to that user.
	 * 
	 * @param checkUserScopes the checkUserScopes to set (default false)
	 */
	public void setCheckUserScopes(boolean checkUserScopes) {
		this.checkUserScopes = checkUserScopes;
	}

	public AuthorizationRequest createAuthorizationRequest(Map<String, String> authorizationParameters) {
logger.trace(authorizationParameters.toString());
		String clientId = authorizationParameters.get(OAuth2Utils.CLIENT_ID);
		String state = authorizationParameters.get(OAuth2Utils.STATE);
		String redirectUri = authorizationParameters.get(OAuth2Utils.REDIRECT_URI);
		Set<String> responseTypes = OAuth2Utils.parseParameterList(
		        decodeParameters(authorizationParameters.get(OAuth2Utils.RESPONSE_TYPE))
		        );

		Set<String> scopes = extractScopes(
		        authorizationParameters,
		        OAuth2Utils.parseParameterList(
                decodeParameters(authorizationParameters.get(OAuth2Utils.SCOPE))),
                clientId);
		
        // workaround to support "id_token" requests
        // TODO fix with proper support in AuthorizationEndpoint
        if (responseTypes.contains(Config.RESPONSE_TYPE_ID_TOKEN)) {
            // ensure one of code or token is requested
            if (!responseTypes.contains(Config.RESPONSE_TYPE_CODE)
                    && !responseTypes.contains(Config.RESPONSE_TYPE_TOKEN)) {
                //treat it like an implicit flow call
                responseTypes.add(Config.RESPONSE_TYPE_TOKEN);
            }

        }
		
		logger.trace("create authorization request for "+clientId
		        +" response "+responseTypes.toString()
                +" scope "+ String.valueOf(authorizationParameters.get(OAuth2Utils.SCOPE))
                +" extracted scope "+scopes.toString()
		        +" redirect "+redirectUri);
		
		// workaround to support "id_token" requests
        // TODO fix with proper support in AuthorizationEndpoint
        if (responseTypes.contains(Config.RESPONSE_TYPE_ID_TOKEN)) {
            // ensure one of code or token is requested
            if (!responseTypes.contains(Config.RESPONSE_TYPE_CODE)
                    && !responseTypes.contains(Config.RESPONSE_TYPE_TOKEN)) {
                //treat it like an implicit flow call
                responseTypes.add(Config.RESPONSE_TYPE_TOKEN);
            }

        }
        
		logger.trace("create authorization request for "+clientId
		        +" response "+responseTypes.toString()
                +" scope "+ String.valueOf(authorizationParameters.get(OAuth2Utils.SCOPE))
                +" extracted scope "+scopes.toString()
		        +" redirect "+redirectUri);
		
		AuthorizationRequest request = new AuthorizationRequest(authorizationParameters,
				Collections.<String, String> emptyMap(), clientId, scopes, null, null, false, state, redirectUri,
				responseTypes);

		if (authorizationParameters.containsKey(NONCE)) {
			request.getExtensions().put(NONCE, authorizationParameters.get(NONCE));
		}
		ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);		
		request.setResourceIdsAndAuthoritiesFromClientDetails(clientDetails);

		return request;

	}

	public OAuth2Request createOAuth2Request(AuthorizationRequest request) {
		return request.createOAuth2Request();
	}

	public TokenRequest createTokenRequest(Map<String, String> requestParameters, ClientDetails authenticatedClient) {

		String clientId = requestParameters.get(OAuth2Utils.CLIENT_ID);
		if (clientId == null) {
			// if the clientId wasn't passed in in the map, we add pull it from the authenticated client object
			clientId = authenticatedClient.getClientId();
		} else {
			// otherwise, make sure that they match
			if (!clientId.equals(authenticatedClient.getClientId())) {
				throw new InvalidClientException("Given client ID does not match authenticated client");
			}
		}
		String grantType = requestParameters.get(OAuth2Utils.GRANT_TYPE);
		Set<String> scopes = new HashSet<>();
		//check grantType and act accordingly to parse scopes
		if(Config.GRANT_TYPE_PASSWORD.equals(grantType) ||
		       Config.GRANT_TYPE_CLIENT_CREDENTIALS.equals(grantType) || 
		       Config.GRANT_TYPE_REFRESH_TOKEN.equals(grantType)) {
		    scopes = extractScopes(requestParameters,
		            OAuth2Utils.parseParameterList(
	                decodeParameters(requestParameters.get(OAuth2Utils.SCOPE))),
	                clientId);
		}
		
        logger.trace("create token request for " + clientId
                + " grantType " + grantType
                + " scope "+ String.valueOf(requestParameters.get(OAuth2Utils.SCOPE))
                + " extracted scope " + scopes.toString());
	      
		TokenRequest tokenRequest = new TokenRequest(requestParameters, clientId, scopes, grantType);

		return tokenRequest;
	}

	public TokenRequest createTokenRequest(AuthorizationRequest authorizationRequest, String grantType) {
		TokenRequest tokenRequest = new TokenRequest(authorizationRequest.getRequestParameters(),
				authorizationRequest.getClientId(), authorizationRequest.getScope(), grantType);
		return tokenRequest;
	}

	public OAuth2Request createOAuth2Request(ClientDetails client, TokenRequest tokenRequest) {
		return tokenRequest.createOAuth2Request(client);
	}
	
    private String decodeParameters(String value) {
        String result = value;
        if (StringUtils.isNotBlank(result)) {

            // check if spaces are still encoded as %20
            if (result.contains("%20")) {
                // replace with spaces
                result = result.replace("%20", " ");
            }
            
            //check if strings are separated with comma (out of spec)
            if(result.contains(",")) {
                result = result.replace(",", " ");
            }
        }

        return result;
    }

    //TODO cleanup requestParameters and rework checkUserScopes
    private Set<String> extractScopes(Map<String, String> requestParameters, Set<String> scopes, String clientId) {
        logger.trace("scopes from parameters "+scopes.toString());
        ClientDetailsEntity clientDetails = clientDetailsRepository.findByClientId(clientId);

        try {
            if ((scopes == null || scopes.isEmpty())) {
                scopes = clientDetails.getScope();
                logger.trace("scopes from client "+scopes.toString());
            }

            boolean addStrongOperationScope = false;
            if (scopes.contains(Config.SCOPE_OPERATION_CONFIRMED)) {
                Object authDetails = SecurityContextHolder.getContext().getAuthentication().getDetails();
                if (authDetails != null && authDetails instanceof AACOAuthRequest) {
                    if (((AACOAuthRequest) authDetails).isMobile2FactorConfirmed()) {
                        addStrongOperationScope = true;
                    }
                    // clear for inappropriate access
                    ((AACOAuthRequest) authDetails).unsetMobile2FactorConfirmed();
                }
                if (!addStrongOperationScope) {
                    throw new InvalidScopeException("The operation.confirmed scope is not authorized by user");
                }
            }

//			boolean requestedOpenidScope = scopes.contains(Config.OPENID_SCOPE);
            
            //check if client wrongly requested "default" with other scopes
            if(scopes.size() > 1 && scopes.contains("default")) {
            	//default is supported only with no other scopes, 
            	//every other scope will include the default claims as well
            	scopes.remove("default");
            }
            
            //filter scopes to those enabled on client
            scopes = checkAvailableScopes(scopes, clientDetails);
            logger.trace("scopes after check available scopes "+scopes.toString());

            //filter scopes to those available to the user
            scopes = checkUserScopes(requestParameters, scopes, clientDetails);
            logger.trace("scopes after check user scopes "+scopes.toString());

            if (addStrongOperationScope) {
                scopes.add(Config.SCOPE_OPERATION_CONFIRMED);
            } else {
                scopes.remove(Config.SCOPE_OPERATION_CONFIRMED);
            }
            if (scopes.isEmpty()) {
                scopes.add("default");
            }
//			if (requestedOpenidScope) scopes.add(Config.OPENID_SCOPE);

        } catch (InvalidScopeException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return scopes;
    }

    private Set<String> checkAvailableScopes(Set<String> requested, ClientDetailsEntity client) throws Exception {
    	Set<String> newScopes = Sets.newHashSet();
    	Set<String> all = client.getScope();
    	//append "default" scopes since those are always supported
    	//TODO move to clientEntity, we actually want these available every time we check
    	all.add("default");
    	all.add(Config.SCOPE_OPERATION_CONFIRMED);
    	
    	//filter
		for (String r : requested) {
			if (all.contains(r)) {
				newScopes.add(r);
			}
		}
		
    	return newScopes;
    }
    
    
    //TODO rework, should get user as param and let authRequest/tokenRequest recover the correct one
	private Set<String> checkUserScopes(Map<String, String> requestParameters, Set<String> scopes, ClientDetailsEntity client) throws Exception {
		Set<String> newScopes = Sets.newHashSet();

		User user = null;
		Long userId = null;
		
		boolean isUser = true;
		if ("client_credentials".equals(requestParameters.get(OAuth2Utils.GRANT_TYPE))/* ||
			"authorization_code".equals(requestParameters.get(OAuth2Utils.GRANT_TYPE))*/) {
			userId = client.getDeveloperId();
			user = userRepository.findOne(userId);
			isUser = false;
		} else if ("password".equals(requestParameters.get(OAuth2Utils.GRANT_TYPE))) {
			String userName = requestParameters.get("username");
			List<User> users = userRepository.findByAttributeEntities(Config.IDP_INTERNAL, "email", userName);
			if (users != null && !users.isEmpty()) {
				user = users.get(0);
			}
		} else {
			try {
				userId = userManager.getUserId();
				user = userRepository.findOne(userId);
			} catch (Exception e) {
				// Not a user ID as expected, live null
			}
		}

		if (user != null) {
		    logger.trace("check user scopes for user "+user.getName());
			newScopes = userManager.userScopes(user, scopes, isUser);
		}

		return newScopes;

		// if (!securityContextAccessor.isUser()) {
		// return scopes;
		// }
		// Set<String> result = new LinkedHashSet<String>();
		// Set<String> authorities =
		// AuthorityUtils.authorityListToSet(securityContextAccessor.getAuthorities());
		// for (String scope : scopes) {
		// if (authorities.contains(scope) ||
		// authorities.contains(scope.toUpperCase())
		// || authorities.contains("ROLE_" + scope.toUpperCase())) {
		// result.add(scope);
		// }
		// }
		// return result;
	}
	
}
