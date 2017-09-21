package it.smartcommunitylab.aac.oauth;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.DefaultSecurityContextAccessor;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.SecurityContextAccessor;
import org.springframework.security.oauth2.provider.TokenRequest;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import it.smartcommunitylab.aac.Config.AUTHORITY;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Resource;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.ResourceRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

public class CustomOAuth2RequestFactory<userManager> implements OAuth2RequestFactory {

	Logger logger = LoggerFactory.getLogger(CustomOAuth2RequestFactory.class);
	
	@Autowired
	private ClientDetailsService clientDetailsService;
	
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;	
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ResourceRepository resourceRepository;	

	@Autowired
	private UserManager userManager;
	
	private SecurityContextAccessor securityContextAccessor = new DefaultSecurityContextAccessor();

	private boolean checkUserScopes = false;

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

		String clientId = authorizationParameters.get(OAuth2Utils.CLIENT_ID);
		String state = authorizationParameters.get(OAuth2Utils.STATE);
		String redirectUri = authorizationParameters.get(OAuth2Utils.REDIRECT_URI);
		Set<String> responseTypes = OAuth2Utils.parseParameterList(authorizationParameters
				.get(OAuth2Utils.RESPONSE_TYPE));

		Set<String> scopes = extractScopes(authorizationParameters, clientId);
		
		AuthorizationRequest request = new AuthorizationRequest(authorizationParameters,
				Collections.<String, String> emptyMap(), clientId, scopes, null, null, false, state, redirectUri,
				responseTypes);

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
		}
		else {
			// otherwise, make sure that they match
			if (!clientId.equals(authenticatedClient.getClientId())) {
				throw new InvalidClientException("Given client ID does not match authenticated client");
			}
		}
		String grantType = requestParameters.get(OAuth2Utils.GRANT_TYPE);

		Set<String> scopes = extractScopes(requestParameters, clientId);
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

	private Set<String> extractScopes(Map<String, String> requestParameters, String clientId) {
		Set<String> scopes = OAuth2Utils.parseParameterList(requestParameters.get(OAuth2Utils.SCOPE));
		ClientDetailsEntity clientDetails = clientDetailsRepository.findByClientId(clientId);

		try {
			ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
			
			if ((scopes == null || scopes.isEmpty())) {
				scopes = clientDetails.getScope();
			}			
			
//			if (client.getParameters() != null) {
			scopes = checkUserScopes(requestParameters, scopes, clientDetails);
//			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return scopes;
	}

	private Set<String> checkUserScopes(Map<String, String> requestParameters, Set<String> scopes, ClientDetailsEntity client) throws Exception {
		Set<String> newScopes = Sets.newHashSet();

		User user = null;
		Long userId = null;
		
		boolean isUser = true;
		if ("client_credentials".equals(requestParameters.get("grant_type"))) {
			userId = client.getDeveloperId();
			user = userRepository.findOne(userId);
			isUser = false;
		} else if ("password".equals(requestParameters.get("grant_type"))) {
			String userName = requestParameters.get("username");
			List<User> users = userRepository.findByAttributeEntities("internal", "email", userName);
			if (users != null && !users.isEmpty()) {
				user = users.get(0);
			}
		} else {
			userId = userManager.getUserId();
			user = userRepository.findOne(userId);
		}

		if (user != null) {
			Set<String> roleNames = user.getRoles().stream().map(x -> x.getRole()).collect(Collectors.toSet());
			for (String scope : scopes) {
				Resource resource = resourceRepository.findByResourceUri(scope);
				if (resource != null) {
					boolean isResourceUser = resource.getAuthority().equals(AUTHORITY.ROLE_USER) || resource.getAuthority().equals(AUTHORITY.ROLE_ANY);
					boolean isResourceClient = !resource.getAuthority().equals(AUTHORITY.ROLE_USER);
					if (isUser && !isResourceUser) {
						continue;
					}
					if (!isUser && !isResourceClient) {
						continue;
					}
					
					if (resource.getRoles() != null && !resource.getRoles().isEmpty()) {
						Set<String> roles = Sets.newHashSet(Splitter.on(",").split(resource.getRoles()));
						if (!Sets.intersection(roleNames, roles).isEmpty()) {
							newScopes.add(scope);
						}
					} else {
						newScopes.add(scope);
					}
				}
			}
		}

		if (newScopes.isEmpty()) {
			newScopes.add("default");
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
