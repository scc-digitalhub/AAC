/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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
 */
package it.smartcommunitylab.aac.controller;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mobile.device.Device;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.AUTHORITY;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.manager.AttributesAdapter;
import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.MobileAuthManager;
import it.smartcommunitylab.aac.manager.ProviderServiceAdapter;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.oauth.AACAuthenticationToken;
import it.smartcommunitylab.aac.oauth.AACOAuthRequest;
import it.smartcommunitylab.aac.repository.UserRepository;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for developer console entry points
 */
@ApiIgnore
@Controller
public class AuthController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${application.url}")
	private String applicationURL;

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ProviderServiceAdapter providerServiceAdapter;
	@Autowired
	private AttributesAdapter attributesAdapter;

	@Autowired
	private ClientDetailsManager clientDetailsAdapter;

	@Autowired
	private RoleManager roleManager;
	
	@Autowired(required=false)
	private RememberMeServices rememberMeServices;
	
	@Autowired(required=false)
	private MobileAuthManager mobileAuthManager;
	
	@Autowired
	private RegistrationManager registrationManager;
	
	private RequestCache requestCache = new HttpSessionRequestCache();

	/**
	 * Redirect to the login type selection page.
	 * 
	 * @param req
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/login")
	public ModelAndView login(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		Map<String, String> authorities = attributesAdapter.getWebAuthorityUrls();
		logger.debug("authorities from adapter: "+authorities.keySet().toString());
		
		SavedRequest savedRequest = requestCache.getRequest(req, res);
		String target = savedRequest != null ? savedRequest.getRedirectUrl() : prepareRedirect(req, "/account");
		req.getSession().setAttribute("redirect", target);
		logger.debug("redirect "+target);
		
		Map<String, String> resultAuthorities = authorities;
		// If original request has client_id parameter, reduce the authorities to the ones of the client app
		if (savedRequest != null) {
			String[] clientIds = savedRequest.getParameterValues(OAuth2Utils.CLIENT_ID);
			if (clientIds != null && clientIds.length > 0) {
				String clientId = clientIds[0];
				
				Set<String> idps = clientDetailsAdapter.getIdentityProviders(clientId);
				String[] loginAuthoritiesParam = savedRequest.getParameterValues("authorities");
				String loginAuthorities = "";
				if (loginAuthoritiesParam != null && loginAuthoritiesParam.length > 0) {
					loginAuthorities = StringUtils.arrayToCommaDelimitedString(loginAuthoritiesParam);
				}
				
				Set<String> all = null;
				if (StringUtils.hasText(loginAuthorities)) {
					all = new HashSet<String>(Arrays.asList(loginAuthorities.split(",")));
				} else {
					all = new HashSet<String>(authorities.keySet());
				}
				resultAuthorities = new HashMap<String, String>();
				for (String idp : all) {
					if (authorities.containsKey(idp) && idps.contains(idp))
						resultAuthorities.put(idp, authorities.get(idp));
				}

				if (resultAuthorities.isEmpty()) {			    
					model.put("message", "No Identity Providers assigned to the app");
					return new ModelAndView("oauth_error", model);
				}
				req.getSession().setAttribute(OAuth2Utils.CLIENT_ID, clientId);
				if (resultAuthorities.size() == 1 && !resultAuthorities.containsKey(Config.IDP_INTERNAL)) {
					return new ModelAndView("redirect:"
							+ Utils.filterRedirectURL(resultAuthorities.keySet().iterator().next()));
				}
				
				//fetch client customizations for login screen
				Map<String,String> customizations = clientDetailsAdapter.getClientCustomizations(clientId);
				model.putAll(customizations);
				
			}
		}
		logger.debug("resultAuthorities "+resultAuthorities.keySet().toString());
		req.getSession().setAttribute("authorities", resultAuthorities);
		
		return new ModelAndView("login", model);
	}

	/**
	 * Entry point for resource access authorization request. Redirects to the
	 * login page. In addition to standard OAuth parameters, it is possible to
	 * specify a comma-separated list of authorities to be used for login as
	 * 'authorities' parameter
	 * 
	 * @param req
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/eauth/authorize")
	public ModelAndView authorise(Device device,
			HttpServletRequest req,
			@RequestParam(value = "authorities", required = false) String loginAuthorities)
			throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();

		String clientId = req.getParameter(OAuth2Utils.CLIENT_ID);
		if (clientId == null || clientId.isEmpty()) {
			model.put("message", "Missing client_id");
			return new ModelAndView("oauth_error", model);
		}
		// each time create new OAuth request
		ClientAppBasic client = clientDetailsAdapter.getByClientId(clientId);
		String clientScopes = StringUtils.collectionToCommaDelimitedString(client.getScope());
		AACOAuthRequest oauthRequest = new AACOAuthRequest(req, device, clientScopes, client.getDisplayName());
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(AUTHORITY.ROLE_USER.toString())) &&
			req.getSession().getAttribute(Config.SESSION_ATTR_AAC_OAUTH_REQUEST) != null) {
			AACOAuthRequest old = (AACOAuthRequest) req.getSession().getAttribute(Config.SESSION_ATTR_AAC_OAUTH_REQUEST);
			oauthRequest.setAuthority(old.getAuthority());
			// update existing session data
			AbstractAuthenticationToken a = new AACAuthenticationToken(auth.getPrincipal(), null, oauthRequest.getAuthority(), auth.getAuthorities());
			a.setDetails(oauthRequest);
			SecurityContextHolder.getContext().setAuthentication(a);
		}
		if (StringUtils.isEmpty(oauthRequest.getAuthority()) && loginAuthorities != null) {
			oauthRequest.setAuthority(loginAuthorities.split(",")[0].trim());
		}
		req.getSession().setAttribute(Config.SESSION_ATTR_AAC_OAUTH_REQUEST, oauthRequest);
		
		String target = prepareRedirect(req, "/eauth/pre-authorize");
		return new ModelAndView("redirect:"+target);
	}

	@RequestMapping("/eauth/pre-authorize")
	public ModelAndView preauthorise(HttpServletRequest req) throws Exception {
		
		AACOAuthRequest oauthRequest = (AACOAuthRequest) req.getSession().getAttribute(Config.SESSION_ATTR_AAC_OAUTH_REQUEST);
		String target = prepareRedirect(req, "/oauth/authorize");
		req.getSession().setAttribute("redirect", target);

		if (oauthRequest != null && oauthRequest.isMobile2FactorRequested()) {
			oauthRequest.unsetMobile2FactorConfirmed();
			return new ModelAndView("forward:/mobile2factor");
		} 
		
		return new ModelAndView("forward:"+target);
	}	
	
	/**
	 * Entry point for resource access authorization request. Redirects to the
	 * login page of the specific identity provider
	 * 
	 * @param req
	 * @param authority
	 *            identity provider alias
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/eauth/authorize/{authority}")
	public ModelAndView authoriseWithAuthority(@PathVariable String authority,
			HttpServletRequest req) throws Exception {
		
		String target = prepareRedirect(req, "/eauth/authorize");
		target += "&authorities="+authority;

		return new ModelAndView("redirect:" + target);
	}

	/**
	 * Endpoint for Access Denied exception page
	 * 
	 * @param req
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/accesserror")
	public ModelAndView accessDenied(HttpServletRequest req) throws Exception {
		return new ModelAndView("accesserror");
	}

	
	/**
	 * Generate redirect string parameter
	 * 
	 * @param req
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	protected String prepareRedirect(HttpServletRequest req, String path)
			{
		String target = path
				+ (req.getQueryString() == null ? "" : "?"
						+ req.getQueryString());
		return target;
	}

	/**
	 * Handles the redirection to the specified target after the login has been
	 * performed. Given the user data collected during the login, updates the
	 * user information in DB and populates the security context with the user
	 * credentials.
	 * 
	 * @param authorityUrl
	 *            the authority used by the user to sign in.
	 * @param target
	 *            target functionality address.
	 * @param req
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	@RequestMapping("/eauth/{authorityUrl}")
	public ModelAndView forward(@PathVariable String authorityUrl,
			@RequestParam(required = false) String target,
			HttpServletRequest req, HttpServletResponse res) {

		String nTarget = (String) req.getSession().getAttribute("redirect");
		if (nTarget == null) {
		    if ("internal".equals(authorityUrl)) {
		        nTarget = prepareRedirect(req, "/account");
		    } else {
		        return new ModelAndView("redirect:/logout");   
		    }			
		}
		String clientId = (String) req.getSession().getAttribute(OAuth2Utils.CLIENT_ID);
		if (clientId != null) {
			Set<String> idps = clientDetailsAdapter
					.getIdentityProviders(clientId);
			if (!idps.contains(authorityUrl)) {
				Map<String, Object> model = new HashMap<String, Object>();
				model.put("message", "incorrect identity provider for the app");
				return new ModelAndView("oauth_error", model);
			}
		}

		AACOAuthRequest oauthRequest = (AACOAuthRequest) req.getSession().getAttribute(Config.SESSION_ATTR_AAC_OAUTH_REQUEST);
		if (oauthRequest != null) {
			oauthRequest.setAuthority(authorityUrl);
			req.getSession().setAttribute(Config.SESSION_ATTR_AAC_OAUTH_REQUEST, oauthRequest);
		}
		
		
		target = nTarget;
		
		Authentication old = SecurityContextHolder.getContext().getAuthentication();
		logger.trace("old auth is "+String.valueOf(old));
		if (old != null && old instanceof AACAuthenticationToken) {
			AACOAuthRequest oldDetails = (AACOAuthRequest) old.getDetails();
			if (oldDetails != null && !authorityUrl.equals(oldDetails.getAuthority())) {
	            new SecurityContextLogoutHandler().logout(req, res, old);
		        SecurityContextHolder.getContext().setAuthentication(null);

				req.getSession().setAttribute("redirect", target);
				req.getSession().setAttribute(OAuth2Utils.CLIENT_ID, clientId);
		        
				return new ModelAndView("redirect:"+Utils.filterRedirectURL(authorityUrl));
			}
		}

		List<NameValuePair> pairs = URLEncodedUtils.parse(URI.create(nTarget), "UTF-8");

		it.smartcommunitylab.aac.model.User userEntity = null;
		if (old instanceof AACAuthenticationToken || old instanceof RememberMeAuthenticationToken) {
			String userId = old.getName();
			userEntity = userRepository.findOne(Long.parseLong(userId));
		} else if ( old instanceof UsernamePasswordAuthenticationToken) {
		    //ensure internal users (logged in via user+password) are given the right identity
		    //avoid impersonation attack via parameters
	        Registration reg = registrationManager.getUserByEmail(old.getName());
	        userEntity = providerServiceAdapter.updateUser(Config.IDP_INTERNAL, toMap(reg), null);
		} else {
			userEntity = providerServiceAdapter.updateUser(authorityUrl, toMap(pairs), req);
		}
		
		logger.trace("userEntity "+userEntity.toString());
		
		List<GrantedAuthority> list = roleManager.buildAuthorities(userEntity);
		
		UserDetails user = new User(userEntity.getId().toString(), "", list);
		AbstractAuthenticationToken a = new AACAuthenticationToken(user, null, authorityUrl, list);
		a.setDetails(oauthRequest);
        logger.trace("new auth "+a.toString());

		SecurityContextHolder.getContext().setAuthentication(a);
		
		if (rememberMeServices != null) {
			rememberMeServices.loginSuccess(req, res, a);
		}
		
		return new ModelAndView("redirect:" + target);
	}

	/**
	 * Mobile 2-factor request endpoint
	 * @param req
	 * @param res
	 * @return provider-specific redirect to an endpoint on a mobile browser
	 */
	@RequestMapping("/mobile2factor")
	public ModelAndView mobile2Factor(HttpServletRequest req, HttpServletResponse res) 
	{
		AACOAuthRequest oauthRequest = (AACOAuthRequest) req.getSession().getAttribute(Config.SESSION_ATTR_AAC_OAUTH_REQUEST);
		if (oauthRequest == null) {
			return new ModelAndView("redirect:/logout");
		}

		String target = mobileAuthManager.init2FactorCheck(req, applicationURL+"/mobile2factor-callback/" + mobileAuthManager.provider());
		return new ModelAndView("redirect:" + target);
	}

	/**
	 * Callback for mobile 2-factor authentication. 
	 * @param req
	 * @param res
	 * @param provider
	 * @return
	 */
	@RequestMapping("/mobile2factor-callback/{provider}")
	public ModelAndView mobile2FactorCallback(HttpServletRequest req, HttpServletResponse res, @PathVariable String provider) 
	{
		AACOAuthRequest oauthRequest = (AACOAuthRequest) req.getSession().getAttribute(Config.SESSION_ATTR_AAC_OAUTH_REQUEST);
		if (oauthRequest == null) {
			return new ModelAndView("redirect:/logout");
		}

		try {
			mobileAuthManager.callback2FactorCheck(req);
			oauthRequest.setMobile2FactorConfirmed();
		} catch (SecurityException e) {
			logger.error("mobile 2 factor auth failed: "+ e.getMessage());
		}
		return new ModelAndView("forward:/eauth/" + oauthRequest.getAuthority());
	}

	/**
	 * @param pairs
	 * @return
	 */
	private Map<String, String> toMap(List<NameValuePair> pairs) {
		if (pairs == null)
			return Collections.emptyMap();
		Map<String, String> map = new HashMap<String, String>();
		for (NameValuePair nvp : pairs) {
			map.put(nvp.getName(), nvp.getValue());
		}
		return map;
	}
	
    private Map<String, String> toMap(Registration existing) {
        Map<String,String> map = new HashMap<String, String>();
        map.put("name", existing.getName());
        map.put("surname", existing.getSurname());
        map.put(Config.USER_ATTR_NAME, existing.getName());
        map.put(Config.USER_ATTR_SURNAME, existing.getSurname());
        map.put("email", existing.getEmail());
        return map;
    }
    
    //DEPRECATED for dedicated RevocationController
//	/**
//	 * Revoke the access token and the associated refresh token.
//	 * 
//	 * @param token
//	 */
//	@RequestMapping("/eauth/revoke/{token}")
//	public @ResponseBody
//	String revokeToken(@PathVariable String token) {
//		OAuth2AccessToken accessTokenObj = tokenStore.readAccessToken(token);
//		if (accessTokenObj != null) {
//			if (accessTokenObj.getRefreshToken() != null) {
//				tokenStore.removeRefreshToken(accessTokenObj.getRefreshToken());
//			}
//			tokenStore.removeAccessToken(accessTokenObj);
//		}
//		return "";
//	}
//	/**
//	 * Revoke the access token and the associated refresh token.
//	 * 
//	 * @param token
//	 */
//	@RequestMapping("/eauth/revoke")
//	public @ResponseBody
//	String revokeTokenWithParam(@RequestParam String token) {
//		OAuth2AccessToken accessTokenObj = tokenStore.readAccessToken(token);
//		if (accessTokenObj != null) {
//			if (accessTokenObj.getRefreshToken() != null) {
//				tokenStore.removeRefreshToken(accessTokenObj.getRefreshToken());
//			}
//			tokenStore.removeAccessToken(accessTokenObj);
//		}
//		return "";
//	}
}