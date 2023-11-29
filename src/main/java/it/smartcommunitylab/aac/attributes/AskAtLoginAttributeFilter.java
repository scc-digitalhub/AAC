package it.smartcommunitylab.aac.attributes;

import static it.smartcommunitylab.aac.attributes.controller.AskAtLoginAttributeController.HOOK_ATTRIBUTES_BASE_URI;
import static it.smartcommunitylab.aac.attributes.controller.AskAtLoginAttributeController.HOOK_BASE;
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

    public static final String ATTRIBUTES_URL_PATTERN = HOOK_ATTRIBUTES_BASE_URI + "/**";
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
    private final RequestMatcher hookAttributesMatcher = new AntPathRequestMatcher(ATTRIBUTES_URL_PATTERN);
    private final RequestMatcher skippedUrlMatcher = buildSkipUrlsMatcher();
    private final RequestMatcher skipOtherHooksMatcher = buildSkipOtherHooksMatcher(); // TODO: eventually move to an appropriate class for shared hook utils

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final RequestCache requestCache = new HttpSessionRequestCache(); // NOTE the state of which attribute are already requested is stored as attribute in the HTTP request
    private final UserService userService;
    private final InternalAttributeAuthority attributeAuthority;
    private final AttributeService attributeService;

    public static final String ATTRIBUTE_SET_COMPLETE = "COMPLETE";
    public static final String ATTRIBUTE_SET_CANCELED = "CANCELED";
    public static final String ATTRIBUTE_SET_STATE = "askAtLoginSetState"; // the state is used to store the information provided in the controller in the current session

    public AskAtLoginAttributeFilter(
        UserService userService,
        AttributeService attributeService,
        InternalAttributeAuthority attributeAuthority
    ) {
        this.userService = userService;
        this.attributeService = attributeService;
        this.attributeAuthority = attributeAuthority;
    }

    private RequestMatcher buildSkipOtherHooksMatcher() {
        // this matcher will match /hook/** but NOT /hook/attributes/**, i.e. it will match "/hook/**" AND (NOT "/hook/attributes/**")
        RequestMatcher baseHookMatcher = new AntPathRequestMatcher(HOOK_BASE + "/**");
        RequestMatcher attributesMatcher = new AntPathRequestMatcher(ATTRIBUTES_URL_PATTERN);
        return new AndRequestMatcher(baseHookMatcher, new NegatedRequestMatcher(attributesMatcher));
    }

    private RequestMatcher buildSkipUrlsMatcher() {
        List<RequestMatcher> antMatchers = Arrays
            .stream(SKIP_URLS)
            .map(AntPathRequestMatcher::new)
            .collect(Collectors.toList());
        return new OrRequestMatcher(antMatchers);
    }

    private boolean isUserAuthenticated() {
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
        if (
            skippedUrlMatcher.matches(request) ||
            skipOtherHooksMatcher.matches(request) ||
            hookAttributesMatcher.matches(request) ||
            !isUserAuthenticated()
        ) {
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
        // check if there is at least one attribute in at least one set in at least one provider that should be asked to user
        for (InternalAttributeService attProvider : attributeProviders) {
            boolean canAskSetsToUser = attProvider.getConfig().isUsermode() && attProvider.getConfig().getAskAtLogin();
            if (canAskSetsToUser) {
                List<String> providerSpecificAttributeSets = attProvider
                    .getConfig()
                    .getAttributeSets()
                    .stream()
                    .sorted()
                    .toList(); // sorted lexicographically
                for (String setId : providerSpecificAttributeSets) {
                    // fetch set definition
                    AttributeSet attSet = null;
                    try {
                        attSet = attributeService.getAttributeSet(setId);
                    } catch (NoSuchAttributeSetException e) {
                        logger.error(
                            "Failed to find or fetch attribute set with id " +
                            setId +
                            ":obtained error with message: " +
                            e.getMessage() +
                            " - skipped filter"
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
                            "Failed to find or fetch attribute values with id " +
                            attSet.getIdentifier() +
                            " for user " +
                            user.getSubjectId() +
                            " -  using empty set instead"
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
                            if (
                                request.getSession().getAttribute(ATTRIBUTE_SET_STATE) != null &&
                                request.getSession().getAttribute(ATTRIBUTE_SET_STATE).equals(ATTRIBUTE_SET_CANCELED)
                            ) {
                                // the user decided to cancel attribute evaluation in this session
                                request.getSession().removeAttribute(ATTRIBUTE_SET_STATE); // cancel attribute otherwise the user is stuck in his decision for all the current session
                                if (requestCache.getRequest(request, response) != null) {
                                    requestCache.removeRequest(request, response);
                                }
                                SecurityContextHolder.clearContext();
                                redirectStrategy.sendRedirect(request, response, "/logout");
                                return;
                            }
                            // store current session (if necessary) to let user resume authentication after he completes
                            if (requestCache.getRequest(request, response) == null) {
                                requestCache.saveRequest(request, response);
                            }
                            // TODO: use an UrlBuilder
                            String redirectFormUri =
                                HOOK_ATTRIBUTES_BASE_URI +
                                String.format("/edit/%s/%s", attProvider.getProvider(), attSet.getIdentifier());
                            logger.debug("Redirect to form filling page {}", redirectFormUri);
                            redirectStrategy.sendRedirect(request, response, redirectFormUri);
                            return;
                        }
                    }
                }
            }
        }

        SavedRequest formerRequest = requestCache.getRequest(request, response);
        String filterState = (String) request.getSession().getAttribute(ATTRIBUTE_SET_STATE);
        if ((filterState != null) && (formerRequest != null)) {
            // the choice was made in this session and we have memory where we were originally going to
            if (filterState.equals(ATTRIBUTE_SET_CANCELED)) {
                throw new RuntimeException("Invalid state"); // sanity check, might remove later on
            }
            String redirectUrl = formerRequest.getRedirectUrl();
            requestCache.removeRequest(request, response);
            redirectStrategy.sendRedirect(request, response, redirectUrl);
            return;
        }
        // go on with the filter chain
        filterChain.doFilter(request, response);
    }
}
