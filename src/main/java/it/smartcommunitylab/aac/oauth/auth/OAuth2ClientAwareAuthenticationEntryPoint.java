package it.smartcommunitylab.aac.oauth.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;

public class OAuth2ClientAwareAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public static final String CLIENT_ID_PARAMETER_NAME = "client_id";

    private final OAuth2ClientDetailsService clientDetailsService;

    public OAuth2ClientAwareAuthenticationEntryPoint(OAuth2ClientDetailsService clientDetailsService,
            String loginFormUrl) {
        super(loginFormUrl);
        Assert.notNull(clientDetailsService, "client details service is required");
        this.clientDetailsService = clientDetailsService;
    }

    @Override
    protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) {

        // check if clientId
        if (StringUtils.hasText(request.getParameter(CLIENT_ID_PARAMETER_NAME))) {
            String clientId = request.getParameter(CLIENT_ID_PARAMETER_NAME);
            try {
                OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);
                String realm = clientDetails.getRealm();

                return "/-/" + realm + getLoginFormUrl() + "?" + CLIENT_ID_PARAMETER_NAME + "=" + clientId;

            } catch (ClientRegistrationException e) {
                // send to error page
                return "/error";
            }
        }

        // return global
        return getLoginFormUrl();

    }

}
