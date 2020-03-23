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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());

//    private static final String URL_TEST = "/testtoken";
    private static final String[] LOCALHOST = { "http://localhost", "http://127.0.0.1" };

//    @Value("${security.redirects.matchports}")
//    private boolean configMatchPorts;

//    @Value("${security.redirects.matchsubdomains}")
//    private boolean configMatchSubDomains;

//    @Value("${application.url}")
    private final String applicationURL;

    private static WhiteboxRedirectResolver localResolver;

    /**
     * @param context
     */
    public ExtRedirectResolver(
            String applicationURL,
            boolean configMatchPorts,
            boolean configMatchSubDomains) {
        super();
        this.applicationURL = applicationURL;
//		path = testTokenPath(context);
        this.setMatchPorts(configMatchPorts);
        this.setMatchSubdomains(configMatchSubDomains);

        // localhost resolver with relaxed check
        localResolver = new WhiteboxRedirectResolver();
        localResolver.setMatchPorts(false);

    }

//	public static String testTokenPath(ServletContext ctx) {
//		return ctx.getContextPath() + URL_TEST;
//	}

//    public static String testTokenPath(String baseURL) {
//        return baseURL + URL_TEST;
//    }

    public static boolean isLocalRedirect(String requestedRedirect, String baseURL) {
        //TODO either remove localhost or put behind flag global or in client
        //we don't want an open redirect for implicit flows
//        Set<String> redirectUris = new HashSet<>(Arrays.asList(LOCALHOST));
//        redirectUris.add(baseURL);

        return localResolver.matchingRedirect(Collections.singleton(baseURL), requestedRedirect);
    }

    @Override
    public String resolveRedirect(String requestedRedirect, ClientDetails client) throws OAuth2Exception {
        logger.trace("check " + requestedRedirect + " against local resolver");
        // match absolute uri for test token
        if (requestedRedirect != null && isLocalRedirect(requestedRedirect, applicationURL)) {
            return requestedRedirect;
        }

        logger.trace("check " + requestedRedirect + " against client redirects");
        return super.resolveRedirect(requestedRedirect, client);
    }

//	@Override
//	protected boolean redirectMatches(String requestedRedirect, String redirectUri) {
//	    //DEPRECATED match path for test token
////		return super.redirectMatches(requestedRedirect, redirectUri) || path.equals(requestedRedirect);
//	    //match absolute uri for test token
//	    return super.redirectMatches(requestedRedirect, redirectUri) || super.redirectMatches(requestedRedirect, path);
//	}

    public static class WhiteboxRedirectResolver extends DefaultRedirectResolver {
        public boolean matchingRedirect(Collection<String> redirectUris, String requestedRedirect) {
            for (String redirectUri : redirectUris) {
                if (requestedRedirect != null && redirectMatches(requestedRedirect, redirectUri)) {
                    return true;
                }
            }

            return false;
        }
    }
}
