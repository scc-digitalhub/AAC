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

package it.smartcommunitylab.aac.controller;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mobile.device.Device;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.ProviderServiceAdapter;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.oauth.AACAuthenticationToken;
import it.smartcommunitylab.aac.oauth.AACOAuthRequest;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author raman
 *
 */
@ApiIgnore
@Controller
@Deprecated
public class LegacyNativeAuthController {

	@Autowired
	private ClientDetailsManager clientDetailsAdapter;
	@Autowired
	private ProviderServiceAdapter providerServiceAdapter;
	@Autowired
	private RoleManager roleManager;
	@Autowired(required=false)
	private RememberMeServices rememberMeServices;

	@RequestMapping("/eauth/authorize/googlelocal")
	public ModelAndView authoriseWithGoogleLocal(Device device, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String authority = "google";
		return processNativeAuth(device, request, response, authority);
	}

	@RequestMapping("/eauth/authorize/facebooklocal")
	public ModelAndView authoriseWithFacebookLocal(Device device, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String authority = "facebook";
		return processNativeAuth(device, request, response, authority);
	}
	@RequestMapping("/eauth/authorize/applelocal")
	public ModelAndView authoriseWithAppleLocal(Device device, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String authority = "apple";
		return processNativeAuth(device, request, response, authority);
	}


	protected ModelAndView processNativeAuth(Device device, HttpServletRequest request, HttpServletResponse response,
			String authority) throws UnsupportedEncodingException {
		Map<String, Object> model = new HashMap<String, Object>();
		String clientId = request.getParameter(OAuth2Utils.CLIENT_ID);
		if (clientId == null || clientId.isEmpty()) {
			model.put("message", "Missing client_id");
			return new ModelAndView("oauth_error", model);
		}
		// each time create new OAuth request
		ClientAppBasic client = clientDetailsAdapter.getByClientId(clientId);
		AACOAuthRequest oauthRequest = new AACOAuthRequest(request, device, client.getScope(), client.getDisplayName());
		
		List<NameValuePair> pairs = URLEncodedUtils.parse(URI.create(request.getRequestURI()+"?"+request.getQueryString()), "UTF-8");

		String target = prepareRedirect(request, "/oauth/authorize");
		it.smartcommunitylab.aac.model.User userEntity = providerServiceAdapter.updateNativeUser(authority, request.getParameter("token"), toMap(pairs));
		List<GrantedAuthority> list = roleManager.buildAuthorities(userEntity);
		
		UserDetails user = new User(userEntity.getId().toString(), "", list);
		AbstractAuthenticationToken a = new AACAuthenticationToken(user, null, authority, list);
		a.setDetails(oauthRequest);
		SecurityContextHolder.getContext().setAuthentication(a);
		
		if (rememberMeServices != null) {
			rememberMeServices.loginSuccess(request, response, a);
		}
		
		return new ModelAndView("redirect:" + target);
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

}
