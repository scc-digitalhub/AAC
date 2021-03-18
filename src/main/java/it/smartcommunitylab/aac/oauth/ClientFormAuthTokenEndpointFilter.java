package it.smartcommunitylab.aac.oauth;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.oauth.ClientFormAuthTokenEndpointFilter.ClientCredentialsRequestMatcher;

/**
 * Filter for the client credential token acquisition. Extends the standard
 * behavior in case of authorization code flow to support PKCE
 * 
 * 
 * @author raman
 *
 */
public class ClientFormAuthTokenEndpointFilter extends
        org.springframework.security.oauth2.provider.client.ClientCredentialsTokenEndpointFilter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String DEFAULT_FILTER_URI = "/oauth/token";

    public ClientFormAuthTokenEndpointFilter() {
        this(DEFAULT_FILTER_URI);
    }

    public ClientFormAuthTokenEndpointFilter(String filterProcessingUrl) {
        super(DEFAULT_FILTER_URI);
        // override request matcher
        RequestMatcher requestMatcher = new ClientCredentialsRequestMatcher(
                new AntPathRequestMatcher(filterProcessingUrl));
        setRequiresAuthenticationRequestMatcher(requestMatcher);
    }

    public ClientFormAuthTokenEndpointFilter(String... filterProcessingUrl) {
        super(DEFAULT_FILTER_URI);
        // override request matcher, we want to support global AND realm paths
        List<RequestMatcher> antMatchers = Arrays.stream(filterProcessingUrl).map(u -> new AntPathRequestMatcher(u))
                .collect(Collectors.toList());
        RequestMatcher requestMatcher = new ClientCredentialsRequestMatcher(new OrRequestMatcher(antMatchers));
        setRequiresAuthenticationRequestMatcher(requestMatcher);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException, IOException, ServletException {

        // we support only POST requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new HttpRequestMethodNotSupportedException(request.getMethod(), new String[] { "POST" });
        }

        String clientId = request.getParameter("client_id");
        String clientSecret = request.getParameter("client_secret");

        // If the request is already authenticated we can assume that this
        // filter is not needed
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication;
        }

        if (clientId == null) {
            throw new BadCredentialsException("No client credentials presented");
        }

        if (clientSecret == null) {
            clientSecret = "";
        }

        clientId = clientId.trim();
        AbstractAuthenticationToken authRequest = new ClientSecretAuthenticationToken(clientId,
                clientSecret);

        // PKCE requests can avoid secret, let's check
        if (!StringUtils.hasText(clientSecret)) {
            String grantType = request.getParameter("grant_type");
            if ("authorization_code".equals(grantType)) {
                String code = request.getParameter("code");
                String verifier = request.getParameter("code_verifier");
                // replace request
                authRequest = new ClientPKCEAuthenticationToken(clientId, code,  verifier);
            }
        }

        // collect request details
        WebAuthenticationDetails webAuthenticationDetails = new WebAuthenticationDetails(request);
        authRequest.setDetails(webAuthenticationDetails);

        // let authManager process request
        return this.getAuthenticationManager().authenticate(authRequest);

    }

    protected static class ClientCredentialsRequestMatcher implements RequestMatcher {
        private RequestMatcher requestMatcher;

        public ClientCredentialsRequestMatcher(RequestMatcher requestMatcher) {
            Assert.notNull(requestMatcher, "request matcher is mandatory");
            this.requestMatcher = requestMatcher;
        }

        @Override
        public boolean matches(HttpServletRequest request) {
            if (requestMatcher.matches(request)) {

                String clientId = request.getParameter("client_id");

                if (clientId == null) {
                    // Give basic auth a chance to work instead (it's preferred anyway)
                    return false;
                }

                // check if basic auth + clientId is provided
                String header = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (StringUtils.startsWithIgnoreCase(header, "Basic")) {
                    // Give basic auth a chance to work instead (it's preferred anyway)
                    return false;
                }

                return true;
            } else {
                return false;
            }
        }
    }
}
