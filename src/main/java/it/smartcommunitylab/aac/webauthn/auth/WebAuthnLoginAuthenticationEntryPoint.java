package it.smartcommunitylab.aac.webauthn.auth;

import it.smartcommunitylab.aac.core.entrypoint.RealmAwarePathUriBuilder;
import it.smartcommunitylab.aac.webauthn.WebAuthnIdentityAuthority;
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

public class WebAuthnLoginAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public static final String DEFAULT_FILTER_URI =
        WebAuthnIdentityAuthority.AUTHORITY_URL +
        "startRegistration/{" +
        WebAuthnLoginAuthenticationEntryPoint.PROVIDER_URI_VARIABLE_NAME +
        "}";
    public static final String DEFAULT_LOGIN_URI =
        WebAuthnIdentityAuthority.AUTHORITY_URL +
        "form/{" +
        WebAuthnLoginAuthenticationEntryPoint.PROVIDER_URI_VARIABLE_NAME +
        "}";
    public static final String SUPER_LOGIN_URI = "/webauthn/authenticate";

    public static final String PROVIDER_URI_VARIABLE_NAME = "registrationId";

    private final String loginFormUrl;
    private RequestMatcher providerRequestMatcher;
    public RealmAwarePathUriBuilder realmUriBuilder;

    public WebAuthnLoginAuthenticationEntryPoint() {
        this(SUPER_LOGIN_URI);
    }

    public WebAuthnLoginAuthenticationEntryPoint(String loginUrl) {
        this(loginUrl, DEFAULT_LOGIN_URI, DEFAULT_FILTER_URI);
    }

    public WebAuthnLoginAuthenticationEntryPoint(String loginUrl, String loginFormUrl, String filterUrl) {
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
        // check via matcher
        if (providerRequestMatcher.matches(request)) {
            // resolve provider
            String provider = providerRequestMatcher.matcher(request).getVariables().get(PROVIDER_URI_VARIABLE_NAME);

            return buildLoginUrl(request, provider);
        }

        // check in parameters
        if (StringUtils.hasText(request.getParameter(PROVIDER_URI_VARIABLE_NAME))) {
            String provider = request.getParameter(PROVIDER_URI_VARIABLE_NAME);
            return buildLoginUrl(request, provider);
        }

        // check in attributes
        if (StringUtils.hasText((String) request.getAttribute(PROVIDER_URI_VARIABLE_NAME))) {
            String provider = (String) request.getAttribute(PROVIDER_URI_VARIABLE_NAME);
            return buildLoginUrl(request, provider);
        }

        // return global login
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
            // return realmUriBuilder.buildUri(request, null,
            // getLoginFormUrl()).expand(params).toUriString();
        }

        return getLoginFormUrl().replaceAll("\\{" + PROVIDER_URI_VARIABLE_NAME + "\\}", provider);
    }
}
