package it.smartcommunitylab.aac.core.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/*
 * Custom login entry point to dispatch requests with the proper security context
 */
public class LoginAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public static final String LOGIN_PATH = "/login";

    public LoginAuthenticationEntryPoint() {
        this(LOGIN_PATH);
    }

    public LoginAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    @Override
    protected String determineUrlToUseForThisRequest(HttpServletRequest request,
            HttpServletResponse response, AuthenticationException exception) {

        String loginUrl = getLoginFormUrl();

        // extract context (realm, client etc)

        return loginUrl;
    }

}
