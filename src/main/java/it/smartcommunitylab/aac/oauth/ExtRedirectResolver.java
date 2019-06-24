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

package it.smartcommunitylab.aac.oauth;

import javax.servlet.ServletContext;import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.provider.endpoint.DefaultRedirectResolver;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;

/**
 * {@link RedirectResolver} implementation with a hook to allow redirects to the authorization server
 * for testing purposes
 * 
 * @author raman
 *
 */
public class ExtRedirectResolver extends DefaultRedirectResolver {

	private static final String URL_TEST = "/testtoken";
	
	private static String path = null;
	
	@Value("${security.redirects.matchports}")
	private boolean configMatchPorts;
	
	@Value("${security.redirects.matchsubdomains}")
    private boolean configMatchSubDomains;
	
	/**
	 * @param context
	 */
	public ExtRedirectResolver(ServletContext context) {
		super();
		path = testTokenPath(context);
		this.setMatchPorts(configMatchPorts);
		this.setMatchSubdomains(configMatchSubDomains);
	}

	public static String testTokenPath(ServletContext ctx) {
		return ctx.getContextPath() + URL_TEST;
	}
	

	@Override
	protected boolean redirectMatches(String requestedRedirect, String redirectUri) {
		return super.redirectMatches(requestedRedirect, redirectUri) || path.equals(requestedRedirect);
	}

	
}
