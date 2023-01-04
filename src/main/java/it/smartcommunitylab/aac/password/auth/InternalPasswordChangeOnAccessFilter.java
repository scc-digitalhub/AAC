package it.smartcommunitylab.aac.password.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.password.provider.InternalPasswordIdentityProviderConfig;

public class InternalPasswordChangeOnAccessFilter extends OncePerRequestFilter {
    static final String SAVED_REQUEST = "INTERNAL_PASSWORD_SAVED_REQUEST";
    static final String[] SKIP_URLS = {
            "/api/**", "/html/**", "/js/**", "/lib/**", "/fonts/**", "/italia/**", "/i18n/**"
    };
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RequestCache requestCache;
    private final RequestMatcher changeRequestMatcher = new AntPathRequestMatcher("/changepwd/**");

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    private RequestMatcher requestMatcher;

    private final ProviderConfigRepository<InternalPasswordIdentityProviderConfig> registrationRepository;
    private final InternalUserPasswordRepository passwordRepository;

    public InternalPasswordChangeOnAccessFilter(InternalUserPasswordRepository passwordRepository,
            ProviderConfigRepository<InternalPasswordIdentityProviderConfig> registrationRepository) {
        Assert.notNull(passwordRepository, "password repository is mandatory");
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");

        this.passwordRepository = passwordRepository;
        this.registrationRepository = registrationRepository;

        // init request cache as store
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        cache.setSessionAttrName(SAVED_REQUEST);
        this.requestCache = cache;

        // build request matcher
        // by default skip static + api requests
        this.requestMatcher = buildRequestMatcher();
    }

    private RequestMatcher buildRequestMatcher() {
        List<RequestMatcher> antMatchers = Arrays.stream(SKIP_URLS)
                .map(u -> new AntPathRequestMatcher(u))
                .collect(Collectors.toList());

        return new NegatedRequestMatcher(new OrRequestMatcher(antMatchers));

    }

    public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
    }

    public void setRequestMatcher(RequestMatcher requestMatcher) {
        this.requestMatcher = requestMatcher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (requestMatcher.matches(request) && requiresProcessing(request) && !changeRequestMatcher.matches(request)) {
            boolean requireChange = false;
            String targetUrl = null;

            logger.trace("process request for {}", request.getRequestURI());

            // fetch user auth and extract reset key tokens
            UserAuthentication userAuth = (UserAuthentication) SecurityContextHolder.getContext().getAuthentication();
            Set<ResetKeyAuthenticationToken> resetTokens = userAuth.getAuthentications().stream()
                    .filter(e -> ResetKeyAuthenticationToken.class.isInstance(e.getToken()))
                    .map(e -> (ResetKeyAuthenticationToken) e.getToken())
                    .collect(Collectors.toSet());

            // check if any token still requires change
            // TODO handle more than one
            if (!resetTokens.isEmpty()) {
                ResetKeyAuthenticationToken token = resetTokens.iterator().next();
                InternalUserAccount account = token.getAccount();
                String providerId = account.getProvider();
                String username = account.getUsername();

                // pick provider config to resolve repositoryId
                // TODO remove and include repositoryId in credentials embedded in auth token
                InternalPasswordIdentityProviderConfig providerConfig = registrationRepository
                        .findByProviderId(providerId);
                if (providerConfig == null) {
                    this.logger.error("Error fetching configuration for active provider");
                    return;
                }

                String repositoryId = providerConfig.getRepositoryId();

                // check if account has an active password
                InternalUserPassword pass = passwordRepository
                        .findByProviderAndUsernameAndStatusOrderByCreateDateDesc(repositoryId, username, "active");
                if (pass == null) {
                    // require change because we still lack a valid password for post-reset login
                    targetUrl = "/changepwd/" + providerId + "/" + account.getUuid();
                    requireChange = true;
                }
            }

            // extract password tokens
            Set<UsernamePasswordAuthenticationToken> passwordTokens = userAuth.getAuthentications().stream()
                    .filter(e -> UsernamePasswordAuthenticationToken.class.isInstance(e.getToken()))
                    .map(e -> (UsernamePasswordAuthenticationToken) e.getToken())
                    .collect(Collectors.toSet());

            // check if any still requires change
            // TODO handle more than one
            if (!passwordTokens.isEmpty()) {
                UsernamePasswordAuthenticationToken token = passwordTokens.iterator().next();
                InternalUserAccount account = token.getAccount();
                String providerId = account.getProvider();
                String username = account.getUsername();

                // pick provider config to resolve repositoryId
                // TODO remove and include repositoryId in credentials embedded in auth token
                InternalPasswordIdentityProviderConfig providerConfig = registrationRepository
                        .findByProviderId(providerId);
                if (providerConfig == null) {
                    this.logger.error("Error fetching configuration for active provider");
                    return;
                }

                String repositoryId = providerConfig.getRepositoryId();

                // check if account has an active password
                InternalUserPassword pass = passwordRepository
                        .findByProviderAndUsernameAndStatusOrderByCreateDateDesc(repositoryId, username, "active");
                if (pass == null) {
                    // error, ignore here
                } else {
                    if (pass.isChangeOnFirstAccess()) {
                        // require change because is set on password
                        targetUrl = "/changepwd/" + providerId + "/" + account.getUuid();
                        requireChange = true;
                    }
                }
            }

            if (requireChange && targetUrl != null) {
                // save request and redirect
                logger.debug("save request to cache");
                this.requestCache.saveRequest(request, response);

                if (response.isCommitted()) {
                    this.logger.debug("Did not redirect to {} since response already committed.", targetUrl);
                    return;
                }

                this.logger.debug("Redirect to {}", targetUrl);
                this.redirectStrategy.sendRedirect(request, response, targetUrl);
                return;
            } else {
                // check if we need to restore a request
                SavedRequest savedRequest = this.requestCache.getRequest(request, response);

                if (savedRequest != null) {
                    logger.debug("restore request from cache");
                    this.requestCache.removeRequest(request, response);
                    this.redirectStrategy.sendRedirect(request, response, savedRequest.getRedirectUrl());
                    return;
                }
            }
        }

        // continue processing
        chain.doFilter(request, response);
        return;
    }

    private boolean requiresProcessing(HttpServletRequest request) {
        // process only authenticated requests
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth instanceof UserAuthentication)) {
            return false;
        }

        // process only if there is a reset key or password token in context
        return ((UserAuthentication) auth).getAuthentications().stream()
                .anyMatch(e -> ResetKeyAuthenticationToken.class.isInstance(e.getToken())
                        || UsernamePasswordAuthenticationToken.class.isInstance(e.getToken()));
    }

}
