package it.smartcommunitylab.aac.oauth.auth;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

public class ClientJwtAssertionAuthenticationConverter extends OAuth2ClientAuthenticationConverter {
    public static final String JWT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    @Override
    public OAuth2ClientJwtAssertionAuthenticationToken attemptConvert(HttpServletRequest request) {

        // fetch and validate parameters
        // NOTE: we always require clientId, but spec says it's OPTIONAL
        // TODO handle extraction from JWT
        // TODO evaluate security risks
        Map<String, String[]> parameters = request.getParameterMap();
        if (!parameters.containsKey(OAuth2ParameterNames.CLIENT_ID)
                || !parameters.containsKey(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE)
                || !parameters.containsKey(OAuth2ParameterNames.CLIENT_ASSERTION)) {
            // not a valid request
            return null;
        }

        String clientAssertionType = request.getParameter(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE);
        if (!JWT_ASSERTION_TYPE.equals(clientAssertionType)) {
            return null;
        }

        // make sure we get exactly 1 value per parameter
        if (parameters.get(OAuth2ParameterNames.CLIENT_ID).length != 1
                || parameters.get(OAuth2ParameterNames.CLIENT_ASSERTION).length != 1) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        // read first (only) value from parameters
        String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
        String clientAssertion = request.getParameter(OAuth2ParameterNames.CLIENT_ASSERTION);

        // let provider evaluate auth method from assertion
        String authenticationMethod = null;

        // validate parameters are *not* empty
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientAssertion)) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
        }

        // return our authRequest
        return new OAuth2ClientJwtAssertionAuthenticationToken(clientId, clientAssertion,
                authenticationMethod);

    }

}
