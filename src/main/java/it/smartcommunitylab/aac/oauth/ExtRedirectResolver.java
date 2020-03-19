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

package it.smartcommunitylab.aac.oauth;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.endpoint.DefaultRedirectResolver;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;

/**
 * {@link RedirectResolver} implementation with a hook to allow redirects to the
 * authorization server for testing purposes
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

    @Value("${application.url}")
    private String applicationURL;

    /**
     * @param context
     */
    public ExtRedirectResolver(ServletContext context) {
        super();
//		path = testTokenPath(context);
        path = testTokenPath(applicationURL);
        this.setMatchPorts(configMatchPorts);
        this.setMatchSubdomains(configMatchSubDomains);
    }

//	public static String testTokenPath(ServletContext ctx) {
//		return ctx.getContextPath() + URL_TEST;
//	}

    public static String testTokenPath(String baseURL) {
        return baseURL + URL_TEST;
    }

    @Override
    public String resolveRedirect(String requestedRedirect, ClientDetails client) throws OAuth2Exception {
        // match absolute uri for test token
        if (requestedRedirect != null && redirectMatches(requestedRedirect, path)) {
            return requestedRedirect;
        }

        return super.resolveRedirect(requestedRedirect, client);
    }

//	@Override
//	protected boolean redirectMatches(String requestedRedirect, String redirectUri) {
//	    //DEPRECATED match path for test token
////		return super.redirectMatches(requestedRedirect, redirectUri) || path.equals(requestedRedirect);
//	    //match absolute uri for test token
//	    return super.redirectMatches(requestedRedirect, redirectUri) || super.redirectMatches(requestedRedirect, path);
//	}

}
