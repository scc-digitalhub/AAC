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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.mobile.device.Device;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.DEVICE_TYPE;

/**
 * @author raman
 *
 */
public class AACOAuthRequest implements Serializable {
	private static final long serialVersionUID = -4784151836171022603L;

	private String clientId;
	private String clientApp;
	private String redirectUri;
	private Set<String> scopes;
	private DEVICE_TYPE device;
	private String grantType;
	private boolean rememberMe;
	private boolean strongAuthConfirmed;
	private String authority;
	
	public AACOAuthRequest(HttpServletRequest request, Device device, String clientScope, String clientApp) {
		clientId = request.getParameter(OAuth2Utils.CLIENT_ID);
		this.clientApp = clientApp;
		grantType = request.getParameter(OAuth2Utils.GRANT_TYPE);
		redirectUri = request.getParameter(OAuth2Utils.REDIRECT_URI);
		String scopeString = request.getParameter(OAuth2Utils.SCOPE);
		if (StringUtils.isEmpty(scopeString)) {
			scopeString = clientScope;
		}
		String[] scopeArr = scopeString.split(",");
		scopes = new HashSet<>();
		for (String s :  scopeArr) {
			scopes.addAll(OAuth2Utils.parseParameterList(s));
		}
		this.device = device.isMobile() 
				? DEVICE_TYPE.MOBILE
				: device.isTablet()
				? DEVICE_TYPE.TABLET
				: device.isNormal() 
				? DEVICE_TYPE.DESKTOP
				: DEVICE_TYPE.UNKNOWN;
		// TODO recognize webview
		
		this.rememberMe = isPortableDevice() || isParamRememberMe(request.getParameter(Config.PARAM_REMEMBER_ME));
	}
	
	/**
	 * @param paramValue
	 * @return
	 */
	private boolean isParamRememberMe(String paramValue) {
		if (paramValue != null) {
			if (paramValue.equalsIgnoreCase("true") || paramValue.equalsIgnoreCase("on")
					|| paramValue.equalsIgnoreCase("yes") || paramValue.equals("1")) {
				return true;
			}
		}
		return false;
	}

	public boolean isPortableDevice() {
		switch(device) {
		case MOBILE:
		case TABLET:
		case WEBVIEW:
			return true;
		default:
			return false;	
		}
	}

	public boolean isWebView() {
		return device.equals(DEVICE_TYPE.WEBVIEW);
	}

	public boolean isMobile2FactorRequested() {
		return isPortableDevice() && !isWebView() && (scopes.contains(Config.SCOPE_OPERATION_CONFIRMED));
	}
	public boolean isMobile2FactorConfirmed() {
		return strongAuthConfirmed;
	}
	public void setMobile2FactorConfirmed() {
		this.strongAuthConfirmed = true;
	}
	public void unsetMobile2FactorConfirmed() {
		this.strongAuthConfirmed = false;
	}
	
	public String getClientId() {
		return clientId;
	}

	public String getRedirectUri() {
		return redirectUri;
	}

	public Set<String> getScopes() {
		return scopes;
	}

	public DEVICE_TYPE getDevice() {
		return device;
	}

	public boolean isRememberMe() {
		return rememberMe;
	}

	public String getGrantType() {
		return grantType;
	}

	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}

	public String getClientApp() {
		return clientApp;
	}
}
