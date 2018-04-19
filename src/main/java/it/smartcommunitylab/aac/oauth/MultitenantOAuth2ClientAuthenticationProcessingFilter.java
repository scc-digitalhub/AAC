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

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2AuthenticationFailureEvent;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetailsSource;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

/**
 * @author raman
 *
 */
public class MultitenantOAuth2ClientAuthenticationProcessingFilter extends OAuth2ClientAuthenticationProcessingFilter {

	
	private ResourceServerTokenServices tokenServices;
	private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new OAuth2AuthenticationDetailsSource();
	private ApplicationEventPublisher eventPublisher;
	private OAuth2ClientDetailsProvider detailsProvider;
	
	private AuthorizationCodeResourceDetails original;
	
	private String id;
	
	/**
	 * Reference to a CheckTokenServices that can validate an OAuth2AccessToken
	 * 
	 * @param tokenServices
	 */
	public void setTokenServices(ResourceServerTokenServices tokenServices) {
		this.tokenServices = tokenServices;
		super.setTokenServices(tokenServices);
	}

	/**
	 * A rest template to be used to obtain an access token.
	 * 
	 * @param restTemplate a rest template
	 */
	public void setRestTemplate(OAuth2RestOperations restTemplate) {
		this.restTemplate = restTemplate;
		super.setRestTemplate(restTemplate);
		this.original = (AuthorizationCodeResourceDetails)restTemplate.getResource();
	}
	
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
		super.setApplicationEventPublisher(eventPublisher);
	}
	/**
	 * @param defaultFilterProcessesUrl
	 */
	public MultitenantOAuth2ClientAuthenticationProcessingFilter(String id, String defaultFilterProcessesUrl, OAuth2ClientDetailsProvider detailsProvider) {
		super(defaultFilterProcessesUrl);
		this.detailsProvider = detailsProvider;
		this.id = id;
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain, Authentication authResult) throws IOException, ServletException {
		super.successfulAuthentication(request, response, chain, authResult);
		// Nearly a no-op, but if there is a ClientTokenServices then the token will now be stored
		getClientTemplate(request).getAccessToken();
	}
	
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {

		OAuth2AccessToken accessToken;
		try {
			accessToken = getClientTemplate(request).getAccessToken();
		} catch (OAuth2Exception e) {
			BadCredentialsException bad = new BadCredentialsException("Could not obtain access token", e);
			publish(new OAuth2AuthenticationFailureEvent(bad));
			throw bad;			
		}
		try {
			OAuth2Authentication result = tokenServices.loadAuthentication(accessToken.getValue());
			if (authenticationDetailsSource!=null) {
				request.setAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE, accessToken.getValue());
				request.setAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_TYPE, accessToken.getTokenType());
				result.setDetails(authenticationDetailsSource.buildDetails(request));
			}
			publish(new AuthenticationSuccessEvent(result));
			return result;
		}
		catch (InvalidTokenException e) {
			BadCredentialsException bad = new BadCredentialsException("Could not obtain user details from token", e);
			publish(new OAuth2AuthenticationFailureEvent(bad));
			throw bad;			
		}
	}
	
	private void publish(ApplicationEvent event) {
		if (eventPublisher!=null) {
			eventPublisher.publishEvent(event);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected OAuth2RestOperations getClientTemplate(HttpServletRequest request) {
		final String clientId = (String) request.getSession().getAttribute(OAuth2Utils.CLIENT_ID);
		ClientDetails clientDetails = detailsProvider.getClientDetails(clientId);
		if (clientDetails == null) {
			throw new BadCredentialsException("Unknown client");
		}
		if (clientDetails.getAdditionalInformation() != null && 
			clientDetails.getAdditionalInformation().containsKey("providerConfigurations") &&
			((Map<String, Object>)clientDetails.getAdditionalInformation().get("providerConfigurations")).get(id) != null) 
		{
			Map<String, String> config = (Map<String, String>) ((Map<String, Object>)clientDetails.getAdditionalInformation().get("providerConfigurations")).get(id);
			if (config.get(OAuth2Utils.CLIENT_ID) != null) {
				AuthorizationCodeResourceDetails resource = new AuthorizationCodeResourceDetails();
				
				resource.setAccessTokenUri(original.getAccessTokenUri());
				resource.setAuthenticationScheme(original.getAuthenticationScheme());
				resource.setClientAuthenticationScheme(original.getClientAuthenticationScheme());
				resource.setGrantType(original.getGrantType());
				resource.setScope(original.getScope());
				resource.setTokenName(original.getTokenName());
				resource.setClientId(config.get(OAuth2Utils.CLIENT_ID));
				resource.setClientSecret(config.get("client_secret"));
				resource.setId(original.getId());
				resource.setUseCurrentUri(original.isUseCurrentUri());
				resource.setUserAuthorizationUri(original.getUserAuthorizationUri());

				resource.setPreEstablishedRedirectUri(original.getPreEstablishedRedirectUri());
				OAuth2RestOperations template = new OAuth2RestTemplate(resource, restTemplate.getOAuth2ClientContext());
				return template;				
			}
		}
		return restTemplate;
	}
	
}
