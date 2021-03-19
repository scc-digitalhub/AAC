package it.smartcommunitylab.aac.core.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

public class RealmAwareAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public static final String REALM_URI_VARIABLE_NAME = "realm";

    private RequestMatcher realmRequestMatcher;

    public RealmAwareAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);

        // build a matcher for realm requests
        realmRequestMatcher = new AntPathRequestMatcher("/-/{" + REALM_URI_VARIABLE_NAME + "}/**");
    }

    @Override
    protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) {

        System.out.println("request for "+String.valueOf(request.getRequestURI()));
        
        // check via matcher
        if (realmRequestMatcher.matches(request)) {
            // resolve realm
            String realm = realmRequestMatcher.matcher(request).getVariables()
                    .get(REALM_URI_VARIABLE_NAME);

            return "/-/" + realm + getLoginFormUrl();
        }

        // check in parameters
        if (StringUtils.hasText(request.getParameter(REALM_URI_VARIABLE_NAME))) {
            String realm = request.getParameter(REALM_URI_VARIABLE_NAME);
            return "/-/" + realm + getLoginFormUrl();
        }

        // return global
        return getLoginFormUrl();

    }

}
