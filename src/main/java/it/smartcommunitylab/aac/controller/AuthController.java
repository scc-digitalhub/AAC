/**
 *    Copyright 2012-2013 Trento RISE
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.manager.AttributesAdapter;
import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.ProviderServiceAdapter;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.oauth.AACAuthenticationToken;
import it.smartcommunitylab.aac.repository.UserRepository;

/**
 * Controller for developer console entry points
 */
@Controller
public class AuthController {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ProviderServiceAdapter providerServiceAdapter;
	@Autowired
	private AttributesAdapter attributesAdapter;

	@Autowired
	private ClientDetailsManager clientDetailsAdapter;

	@Autowired
	private TokenStore tokenStore;

	@Autowired
	private RoleManager roleManager;
	
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

		SavedRequest savedRequest = requestCache.getRequest(req, res);
		String target = savedRequest != null ? savedRequest.getRedirectUrl() : prepareRedirect(req, "/dev");
		req.getSession().setAttribute("redirect", target);
		
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
				req.getSession().setAttribute("client_id", clientId);
				if (resultAuthorities.size() == 1) {
					return new ModelAndView("redirect:"
							+ Utils.filterRedirectURL(resultAuthorities.keySet().iterator().next()));
				}
			}
		}
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
	public ModelAndView authorise(
			HttpServletRequest req,
			@RequestParam(value = "authorities", required = false) String loginAuthorities)
			throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();

		String clientId = req.getParameter("client_id");
		if (clientId == null || clientId.isEmpty()) {
			model.put("message", "Missing client_id");
			return new ModelAndView("oauth_error", model);
		}
		
		String target = prepareRedirect(req, "/oauth/authorize");
		return new ModelAndView("redirect:"+target);
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
	 * @param authority
	 *            identity provider alias
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
			throws UnsupportedEncodingException {
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
	@RequestMapping("/eauth/{authorityUrl}")
	public ModelAndView forward(@PathVariable String authorityUrl,
			@RequestParam(required = false) String target,
			HttpServletRequest req, HttpServletResponse res) throws Exception {

		String nTarget = (String) req.getSession().getAttribute("redirect");
		if (nTarget == null)
			return new ModelAndView("redirect:/logout");

		String clientId = (String) req.getSession().getAttribute("client_id");
		if (clientId != null) {
			Set<String> idps = clientDetailsAdapter
					.getIdentityProviders(clientId);
			if (!idps.contains(authorityUrl)) {
				Map<String, Object> model = new HashMap<String, Object>();
				model.put("message", "incorrect identity provider for the app");
				return new ModelAndView("oauth_error", model);
			}
		}

		target = nTarget;
		
		Authentication old = SecurityContextHolder.getContext().getAuthentication();
		if (old != null && old instanceof AACAuthenticationToken) {
			if (!authorityUrl.equals(old.getDetails())) {
	            new SecurityContextLogoutHandler().logout(req, res, old);
		        SecurityContextHolder.getContext().setAuthentication(null);

				req.getSession().setAttribute("redirect", target);
				req.getSession().setAttribute("client_id", clientId);
		        
				return new ModelAndView("redirect:"+Utils.filterRedirectURL(authorityUrl));
			}
		}

		List<NameValuePair> pairs = URLEncodedUtils.parse(URI.create(nTarget), "UTF-8");

		it.smartcommunitylab.aac.model.User userEntity = null;
		if (old != null && old instanceof AACAuthenticationToken) {
			String userId = old.getName();
			userEntity = userRepository.findOne(Long.parseLong(userId));
		} else {
			userEntity = providerServiceAdapter.updateUser(authorityUrl, toMap(pairs), req);
		}

		List<GrantedAuthority> list = roleManager.buildAuthorities(userEntity);
		
		UserDetails user = new User(userEntity.getId().toString(), "", list);
		AbstractAuthenticationToken a = new AACAuthenticationToken(user, null, authorityUrl, list);
		a.setDetails(authorityUrl);

		SecurityContextHolder.getContext().setAuthentication(a);

		return new ModelAndView("redirect:" + target);
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

	/**
	 * Revoke the access token and the associated refresh token.
	 * 
	 * @param token
	 */
	@RequestMapping("/eauth/revoke/{token}")
	public @ResponseBody
	String revokeToken(@PathVariable String token) {
		OAuth2AccessToken accessTokenObj = tokenStore.readAccessToken(token);
		if (accessTokenObj != null) {
			if (accessTokenObj.getRefreshToken() != null) {
				tokenStore.removeRefreshToken(accessTokenObj.getRefreshToken());
			}
			tokenStore.removeAccessToken(accessTokenObj);
		}
		return "";
	}

}