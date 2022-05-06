package it.smartcommunitylab.aac.oauth.auth;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;

public class ClientRefreshAuthenticationConverter extends OAuth2ClientAuthenticationConverter {

    @Override
    public OAuth2ClientRefreshAuthenticationToken attemptConvert(HttpServletRequest request) {
        // we support only POST requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        // fetch and validate parameters
        Map<String, String[]> parameters = request.getParameterMap();
        if (!parameters.containsKey(OAuth2ParameterNames.CLIENT_ID)
                || !parameters.containsKey(OAuth2ParameterNames.REFRESH_TOKEN)
                || !parameters.containsKey(OAuth2ParameterNames.GRANT_TYPE)) {
            // not a valid request
            return null;
        }

        // support refresh flow without secret for public clients
        // requires refresh token rotation set
        AuthorizationGrantType grantType = AuthorizationGrantType
                .parse(request.getParameter(OAuth2ParameterNames.GRANT_TYPE));

        if (AuthorizationGrantType.REFRESH_TOKEN != grantType) {
            return null;
        }

        // make sure we get exactly 1 value per parameter
        if (parameters.get(OAuth2ParameterNames.CLIENT_ID).length != 1
                || parameters.get(OAuth2ParameterNames.REFRESH_TOKEN).length != 1) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        // read first (only) value from parameters
        String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
        String refreshToken = request.getParameter(OAuth2ParameterNames.REFRESH_TOKEN);

        // validate parameters are *not* empty
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(refreshToken)) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
        }

        // return our authRequest
        return new OAuth2ClientRefreshAuthenticationToken(clientId, refreshToken, AuthenticationMethod.NONE.getValue());

    }

}
