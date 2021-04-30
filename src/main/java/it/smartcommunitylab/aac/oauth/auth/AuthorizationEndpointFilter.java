package it.smartcommunitylab.aac.oauth.auth;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import it.smartcommunitylab.aac.core.auth.ComposedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.oauth.client.OAuth2Client;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientService;

public class AuthorizationEndpointFilter extends OncePerRequestFilter {

    public static final String DEFAULT_FILTER_URI = "/oauth/authorize";

    private final RequestMatcher requestMatcher;

    // we need access to client
    private final OAuth2ClientService clientService;

    public AuthorizationEndpointFilter(OAuth2ClientService clientService) {
        this(clientService, DEFAULT_FILTER_URI);
    }

    public AuthorizationEndpointFilter(OAuth2ClientService clientService, String filterProcessingUrl) {
        Assert.notNull(clientService, "client service is required");
        Assert.hasText(filterProcessingUrl, "filter url can not be null or empty");
        this.clientService = clientService;
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (requestMatcher.matches(request) && requiresProcessing(request)) {

            UserAuthenticationToken userAuth = getUserAuthentication();
            String clientId = request.getParameter("client_id");

            // load client
            OAuth2Client client = clientService.findClient(clientId);

            // we ignore errors, don't want to unauthenticate users on non existing
            // clients..
            if (client != null && userAuth != null) {
                // load idps
                Set<String> providers = client.getProviders();

                // user auth needs to contain at least one token from a client approved idp
                // TODO handle COMMON realm requests, for now only same realm is supported

                Set<String> userIdps = userAuth.getAuthentications().stream()
                        .map(e -> e.getProvider())
                        .filter(p -> providers.contains(p))
                        .collect(Collectors.toSet());

                if (userIdps.isEmpty()) {
                    // user has no matching auth, we clear the context to trigger reauth
                    SecurityContextHolder.clearContext();
                }

            }
        }
        
        //always continue processing
        chain.doFilter(request, response);
        return;

    }

    private boolean requiresProcessing(HttpServletRequest request) {
        boolean hasParam = StringUtils.hasText(request.getParameter("client_id"));
        boolean hasAuth = !(getUserAuthentication() == null);

        return hasParam && hasAuth;
    }

    private UserAuthenticationToken getUserAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UserAuthenticationToken) {
            return (UserAuthenticationToken) auth;
        }
        if (auth instanceof ComposedAuthenticationToken) {
            return ((ComposedAuthenticationToken) auth).getUserAuthentication();
        } else {
            return null;
        }

    }
}
