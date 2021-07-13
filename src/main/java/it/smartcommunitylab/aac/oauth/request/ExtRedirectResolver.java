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

package it.smartcommunitylab.aac.oauth.request;

import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.common.exceptions.RedirectMismatchException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.endpoint.DefaultRedirectResolver;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

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
    private static final String LOCALHOST = "http://localhost";

//    @Value("${security.redirects.matchports}")
//    private boolean configMatchPorts;

//    @Value("${security.redirects.matchsubdomains}")
//    private boolean configMatchSubDomains;

//    @Value("${application.url}")
    private final String applicationURL;

    private static LocalhostRedirectResolver localResolver;

    /**
     * @param context
     */
    public ExtRedirectResolver(
            String applicationURL,
            boolean configMatchPorts,
            boolean configMatchSubDomains) {
        super();
        this.applicationURL = applicationURL;
        this.setMatchPorts(configMatchPorts);
        this.setMatchSubdomains(configMatchSubDomains);

        // localhost resolver with relaxed check on ports
        localResolver = new LocalhostRedirectResolver();
    }

    @Override
    public String resolveRedirect(String requestedRedirect, ClientDetails client) throws OAuth2Exception {
        logger.trace("check " + requestedRedirect + " against localhost resolver");
        // match localhost first
        try {
            return localResolver.resolveRedirect(requestedRedirect, client);
        } catch (RedirectMismatchException e) {
        }

        logger.trace("check " + requestedRedirect + " against client redirects");
        return super.resolveRedirect(requestedRedirect, client);
    }

    public static class LocalhostRedirectResolver extends DefaultRedirectResolver {
        public LocalhostRedirectResolver() {
            super();
            setMatchPorts(false);
            setMatchSubdomains(true);
        }

        protected boolean redirectMatches(String requestedRedirect, String redirectUri) {
            if (StringUtils.startsWithIgnoreCase(redirectUri, LOCALHOST)) {
                return super.redirectMatches(requestedRedirect, redirectUri);
            }

            return false;
        }

    }
}
