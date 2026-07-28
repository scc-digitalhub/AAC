package it.smartcommunitylab.aac.otp.auth;

import it.smartcommunitylab.aac.core.entrypoint.RealmAwarePathUriBuilder;
import it.smartcommunitylab.aac.otp.OtpIdentityAuthority;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;

/**
 * Authentication entry point for OTP-based flows.
 * Handles redirection to the OTP login form when authentication is required.
 */

public class InternalOtpLoginAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public static final String DEFAULT_FILTER_URI = OtpIdentityAuthority.AUTHORITY_URL + "login/{registrationId}";
    public static final String DEFAULT_LOGIN_URI = OtpIdentityAuthority.AUTHORITY_URL + "form/{registrationId}";
    public static final String SUPER_LOGIN_URI = "/login";

    public static final String PROVIDER_URI_VARIABLE_NAME = "registrationId";

    private final String loginFormUrl;
    private RequestMatcher providerRequestMatcher;
    public RealmAwarePathUriBuilder realmUriBuilder;

    public InternalOtpLoginAuthenticationEntryPoint() {
        this(SUPER_LOGIN_URI);
    }

    public InternalOtpLoginAuthenticationEntryPoint(String loginUrl) {
        this(loginUrl, DEFAULT_LOGIN_URI, DEFAULT_FILTER_URI);
    }

    public InternalOtpLoginAuthenticationEntryPoint(String loginUrl, String loginFormUrl, String filterUrl) {
        super(loginUrl);
        this.loginFormUrl = loginFormUrl;

        // build a matcher for realm requests
        providerRequestMatcher = new AntPathRequestMatcher(filterUrl);
    }

    public void setProviderRequestMatcher(RequestMatcher providerRequestMatcher) {
        this.providerRequestMatcher = providerRequestMatcher;
    }

    public void setRealmUriBuilder(RealmAwarePathUriBuilder realmUriBuilder) {
        this.realmUriBuilder = realmUriBuilder;
    }

    @Override
    protected String determineUrlToUseForThisRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) {
        // Attempt to resolve provider ID via URL path matching
        if (providerRequestMatcher.matches(request)) {
            // Extract provider ID from path variable
            String provider = providerRequestMatcher.matcher(request).getVariables().get(PROVIDER_URI_VARIABLE_NAME);

            return buildLoginUrl(request, provider);
        }

        // Fallback: Check for registrationId in query parameters
        if (StringUtils.hasText(request.getParameter(PROVIDER_URI_VARIABLE_NAME))) {
            String provider = request.getParameter(PROVIDER_URI_VARIABLE_NAME);
            return buildLoginUrl(request, provider);
        }

        // Fallback: Check for registrationId in request attributes
        if (StringUtils.hasText((String) request.getAttribute(PROVIDER_URI_VARIABLE_NAME))) {
            String provider = (String) request.getAttribute(PROVIDER_URI_VARIABLE_NAME);
            return buildLoginUrl(request, provider);
        }

        // Return global login URL if no provider identified
        return super.getLoginFormUrl();
    }

    @Override
    public String getLoginFormUrl() {
        return this.loginFormUrl;
    }

    private String buildLoginUrl(HttpServletRequest request, String provider) {
        if (realmUriBuilder != null) {
            Map<String, String> params = Collections.singletonMap(PROVIDER_URI_VARIABLE_NAME, provider);
            UriComponents u1 = realmUriBuilder.buildUri(request, null, getLoginFormUrl());
            UriComponents u2 = u1.expand(params);
            String u = u2.toUriString();
            return u;
        }

        return getLoginFormUrl().replaceAll("\\{" + PROVIDER_URI_VARIABLE_NAME + "\\}", provider);
    }
}
