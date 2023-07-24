package it.smartcommunitylab.aac.oauth.auth;

import it.smartcommunitylab.aac.core.auth.LoginUrlRequestConverter;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OAuth2ClientAwareLoginUrlConverter implements LoginUrlRequestConverter {

    public static final String CLIENT_ID_PARAMETER_NAME = "client_id";

    private final OAuth2ClientDetailsService clientDetailsService;
    private String loginUrl;

    public OAuth2ClientAwareLoginUrlConverter(OAuth2ClientDetailsService clientDetailsService, String loginFormUrl) {
        Assert.notNull(clientDetailsService, "clientDetails service is required");
        this.clientDetailsService = clientDetailsService;
        this.loginUrl = loginFormUrl;
    }

    @Override
    public String convert(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) {
        // check if clientId via param
        String clientId = null;
        if (request.getParameter(CLIENT_ID_PARAMETER_NAME) != null) {
            clientId = request.getParameter(CLIENT_ID_PARAMETER_NAME);
        }

        // check if clientId via attribute
        if (request.getAttribute(CLIENT_ID_PARAMETER_NAME) != null) {
            clientId = (String) request.getAttribute(CLIENT_ID_PARAMETER_NAME);
        }
        // check if clientId
        if (StringUtils.hasText(clientId)) {
            try {
                OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);
                String realm = clientDetails.getRealm();

                return "/-/" + realm + loginUrl + "?" + CLIENT_ID_PARAMETER_NAME + "=" + clientId;
            } catch (ClientRegistrationException e) {
                return null;
            }
        }

        // not found
        return null;
    }
}
