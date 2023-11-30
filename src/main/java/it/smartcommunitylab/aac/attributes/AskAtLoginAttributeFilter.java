package it.smartcommunitylab.aac.attributes;

import static it.smartcommunitylab.aac.attributes.controller.AskAtLoginAttributeController.*;
import static it.smartcommunitylab.aac.tos.TosOnAccessFilter.TERMS_URL_PATTERN;

import it.smartcommunitylab.aac.attributes.model.Attribute;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.internal.InternalAttributeAuthority;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeService;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.users.service.UserService;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
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
import org.springframework.security.web.util.matcher.*;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class AskAtLoginAttributeFilter extends OncePerRequestFilter {

    public static final String ATTRIBUTE_SET_COMPLETE = "COMPLETE";
    public static final String ATTRIBUTE_SET_CANCELED = "CANCELED";
    public static final String ATTRIBUTE_SET_STATE = "hookAttributeSingleSetState"; // this state is evaluated by the associated Controller
    static final String[] SKIP_URLS = {
        "/api/**",
        "/html/**",
        "/js/**",
        "/lib/**",
        "/fonts/**",
        "/italia/**",
        "/i18n/**",
        // entries required in order to not intercept the behaviour resolved by other filters
        TERMS_URL_PATTERN,
        "/error",
    };
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RequestMatcher skippedUrlMatcher = buildSkipUrlsMatcher();
    private final RequestMatcher skipHooksMatcher = new AntPathRequestMatcher(HOOK_BASE + "/**"); // TODO: eventually move to an appropriate class for shared hook utils

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final RequestCache requestCache;
    private final UserService userService;
    private final InternalAttributeAuthority attributeAuthority;
    private final AttributeService attributeService;

    public AskAtLoginAttributeFilter(
        UserService userService,
        AttributeService attributeService,
        InternalAttributeAuthority attributeAuthority
    ) {
        this.userService = userService;
        this.attributeService = attributeService;
        this.attributeAuthority = attributeAuthority;
        this.requestCache = new HttpSessionRequestCache();
    }

    private RequestMatcher buildSkipUrlsMatcher() {
        List<RequestMatcher> antMatchers = Arrays
            .stream(SKIP_URLS)
            .map(AntPathRequestMatcher::new)
            .collect(Collectors.toList());
        return new OrRequestMatcher(antMatchers);
    }

    /**
     * Requires processing when:
     *  1. user is authenticated
     * @return true is filter requires processing, false otherwise
     */
    private boolean requiresProcessing() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof UserAuthentication)) {
            return false;
        }
        UserAuthentication userAuth = (UserAuthentication) SecurityContextHolder.getContext().getAuthentication();
        ExtendedAuthenticationToken token = CollectionUtils.firstElement(userAuth.getAuthentications());

        if (token == null) {
            logger.error("empty token on authentication success");
            return false;
        }

        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (skippedUrlMatcher.matches(request) || skipHooksMatcher.matches(request) || !requiresProcessing()) {
            filterChain.doFilter(request, response);
            return;
        }
        UserAuthentication userAuth = (UserAuthentication) SecurityContextHolder.getContext().getAuthentication();
        ExtendedAuthenticationToken token = CollectionUtils.firstElement(userAuth.getAuthentications());
        String realm = token.getRealm();
        List<InternalAttributeService> attributeProviders = attributeAuthority.getProvidersByRealm(realm);
        if ((attributeProviders == null) || attributeProviders.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        User user = userService.findUser(userAuth.getSubjectId());

        // handle completion of a previous set
        SavedRequest formerRequest = requestCache.getRequest(request, response);
        String hookState = (String) request.getSession().getAttribute(ATTRIBUTE_SET_STATE);
        if (formerRequest != null) {
            if ((hookState != null) && (hookState.equals(ATTRIBUTE_SET_COMPLETE))) {
                // state: an attribute set was evaluated in this session
                request.getSession().removeAttribute(ATTRIBUTE_SET_STATE);
                String redirectUrl = formerRequest.getRedirectUrl();
                logger.debug("recovered request from cache");
                requestCache.removeRequest(request, response);
                redirectStrategy.sendRedirect(request, response, redirectUrl);
                return;
            }
            if ((hookState != null) && (hookState.equals(ATTRIBUTE_SET_CANCELED))) {
                // an operation to complete an attribute was canceled in this session - clear security context
                request.getSession().removeAttribute(ATTRIBUTE_SET_STATE); // cancel attribute otherwise the user is stuck in his decision for all the current session
                requestCache.removeRequest(request, response);
                SecurityContextHolder.clearContext();
                redirectStrategy.sendRedirect(request, response, "/logout");
                return;
            }
            // no behaviour defined for cached filled but missing state
        }

        // check if there is one attribute in at least one set in at least one provider that should be asked to user
        for (InternalAttributeService attProvider : attributeProviders) {
            boolean canAskSetsToUser = attProvider.getConfig().isUsermode() && attProvider.getConfig().getAskAtLogin();
            if (canAskSetsToUser) {
                List<String> providerSpecificAttributeSets = attProvider
                    .getConfig()
                    .getAttributeSets()
                    .stream()
                    .sorted() // sorted lexicographically
                    .toList();
                for (String setId : providerSpecificAttributeSets) {
                    // fetch set definition
                    AttributeSet attSet = null;
                    try {
                        attSet = attributeService.getAttributeSet(setId);
                    } catch (NoSuchAttributeSetException e) {
                        logger.error(
                            String.format(
                                "Failed to find or fetch attribute set with id %s: obtained error with message: %s - skipped filter",
                                setId,
                                e.getMessage()
                            )
                        );
                        filterChain.doFilter(request, response);
                        return;
                    }
                    // obtain which attributes in the set are currently evaluated
                    Map<String, Serializable> currentAttributeValuesMap = new HashMap<>();
                    UserAttributes attValues = null;
                    try {
                        attValues = attProvider.getUserAttributes(user.getSubjectId(), attSet.getIdentifier());
                    } catch (NoSuchAttributeSetException e) {
                        // go on and assume that value set is empty
                        logger.warn(
                            String.format(
                                "Failed to find or fetch attribute values with id %s for user %s",
                                attSet.getIdentifier(),
                                user.getSubjectId()
                            )
                        );
                    }
                    if (attValues != null) {
                        for (Attribute att : attValues.getAttributes()) {
                            String key = att.getKey();
                            Serializable value = att.getValue(); // null value is ok
                            if (key != null) {
                                currentAttributeValuesMap.put(key, value);
                            }
                        }
                    }
                    // iterate on the attribute set to check if there is at least 1 non-evaluated mandatory attribute which can be asked to user
                    for (Attribute att : attSet.getAttributes()) {
                        if (att.getIsRequired() && currentAttributeValuesMap.get(att.getKey()) == null) {
                            // ALWAYS store current session to let user resume authentication after he completes
                            requestCache.saveRequest(request, response);
                            // TODO: use an UrlBuilder
                            String redirectFormUri =
                                HOOK_ATTRIBUTES_BASE_URI +
                                String.format(HOOK_ATTRIBUTES_FORM_FORMAT, attProvider.getProvider(), attSet.getIdentifier());
                            logger.debug("Redirect to form filling page {}", redirectFormUri);
                            redirectStrategy.sendRedirect(request, response, redirectFormUri);
                            return;
                        }
                    }
                }
            }
        }

        // No attribute required - go on with the filter chain
        filterChain.doFilter(request, response);
    }
}
