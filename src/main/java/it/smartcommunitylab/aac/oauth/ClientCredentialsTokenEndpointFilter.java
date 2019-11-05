package it.smartcommunitylab.aac.oauth;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * Filter for the client credential token acquisition. Extends the standard behaviour
 * in case of authorization code flow by checking also the 'mobile' client secret against
 * the requested one.
 * @author raman
 *
 */
public class ClientCredentialsTokenEndpointFilter extends
	org.springframework.security.oauth2.provider.client.ClientCredentialsTokenEndpointFilter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	protected ClientDetailsRepository clientDetailsRepository = null;

	private boolean allowOnlyPost = false;

	@Override
	public void setAllowOnlyPost(boolean allowOnlyPost) {
		this.allowOnlyPost = allowOnlyPost;
	}

	public ClientCredentialsTokenEndpointFilter(ClientDetailsRepository clientDetailsRepository) {
		super();
		this.clientDetailsRepository = clientDetailsRepository;
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request,
			HttpServletResponse response) throws AuthenticationException, IOException, ServletException {

		if (allowOnlyPost && !"POST".equalsIgnoreCase(request.getMethod())) {
			throw new HttpRequestMethodNotSupportedException(request.getMethod(), new String[] { "POST" });
		}

		// If the request is already authenticated we can assume that this
		// filter is not needed
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()) {
			return authentication;
		}

		String clientId = request.getParameter(OAuth2Utils.CLIENT_ID);
		String clientSecret = request.getParameter("client_secret");

		if (clientId == null) {
			throw new BadCredentialsException("No client credentials presented");
		}

		clientId = clientId.trim();

		ClientDetailsEntity clientDetails = clientDetailsRepository.findByClientId(clientId);
		if (clientDetails == null) {
			throw new BadCredentialsException("No client found");
		}

		String grant_type = request.getParameter(OAuth2Utils.GRANT_TYPE);
		Set<String> grantTypes = checkGrantTypes(clientDetails, grant_type);
		
		 //check if trusted client for password-based access
		if ("password".equals(grant_type)) {
			checkInternalIdP(clientDetails);
		}
		String clientSecretServer = clientDetails.getClientSecret();
		// specific case: PKCE allows for not having client_secret. Will be checked by granter
		if (StringUtils.isEmpty(clientSecret) && "authorization_code".equals(grant_type)) {
			String verifier = request.getParameter(AACOAuth2Utils.CODE_VERIFIER);
			if (StringUtils.isEmpty(verifier)) {
			    throw new BadCredentialsException(messages.getMessage(
			            "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
			} else {
				return createAuthentication(clientId, clientDetails, clientSecretServer);
			}
		}
		
		if ("authorization_code".equals(grant_type) || "refresh_token".equals(grant_type) || "password".equals(grant_type) || "native".equals(grant_type)) {
			checkCredentialsWithMobile(clientSecret, clientDetails, grantTypes, clientSecretServer);
		} else {
			checkCredentials(clientSecret, clientSecretServer);
		}
		return createAuthentication(clientId, clientDetails, clientSecretServer);
	}

	protected void checkCredentials(String clientSecret, String clientSecretServer) {
		if (!clientSecretServer.equals(clientSecret)) {
		    throw new BadCredentialsException(messages.getMessage(
		            "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
		}
	}

	protected void checkCredentialsWithMobile(String clientSecret, ClientDetailsEntity clientDetails,
			Set<String> grantTypes, String clientSecretServer) {
		String clientSecretMobile = clientDetails.getClientSecretMobile();
		if (clientSecretMobile.equals(clientSecret) && !grantTypes.contains(Config.GRANT_TYPE_NATIVE)) {
			throw new BadCredentialsException("Native app access is not enabled");
		}
		
		if (!clientSecretServer.equals(clientSecret) && !clientSecretMobile.equals(clientSecret)) {
		    throw new BadCredentialsException(messages.getMessage(
		            "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
		}
	}
	
	protected UsernamePasswordAuthenticationToken createAuthentication(String clientId, ClientDetailsEntity clientDetails, String clientSecretServer) {
		UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(clientId, clientSecretServer);
		User user = new User(clientId, clientSecretServer, clientDetails.getAuthorities());
        UsernamePasswordAuthenticationToken result = 
        		new UsernamePasswordAuthenticationToken(user,
                authRequest.getCredentials(), user.getAuthorities());
        result.setDetails(authRequest.getDetails());
		return result;
	}


	protected void checkInternalIdP(ClientDetailsEntity clientDetails) {
		ClientAppInfo info = ClientAppInfo.convert(clientDetails.getAdditionalInformation());
		if (!info.getIdentityProviders().containsKey(Config.IDP_INTERNAL) ||
		   ClientAppInfo.APPROVED != info.getIdentityProviders().get(Config.IDP_INTERNAL)) 
		{
			throw new BadCredentialsException("Unauthorized ause of internal IdP");				
		}
	}

	protected Set<String> checkGrantTypes(ClientDetailsEntity clientDetails, String grant_type) {
		Set<String> grantTypes = clientDetails.getAuthorizedGrantTypes();
		if (grantTypes == null || !grantTypes.contains(grant_type)) {
			throw new BadCredentialsException("Unauthorized grant type: " + grant_type);
		}
		return grantTypes;
	}


	
	
}
