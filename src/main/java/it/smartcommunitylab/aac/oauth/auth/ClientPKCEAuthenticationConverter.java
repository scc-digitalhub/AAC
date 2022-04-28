package it.smartcommunitylab.aac.oauth.auth;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;

public class ClientPKCEAuthenticationConverter extends OAuth2ClientAuthenticationConverter {

    @Override
    public OAuth2ClientPKCEAuthenticationToken attemptConvert(HttpServletRequest request) {
        // we support only POST requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        // fetch and validate parameters
        Map<String, String[]> parameters = request.getParameterMap();
        if (!parameters.containsKey(OAuth2ParameterNames.CLIENT_ID)
                || !parameters.containsKey(PkceParameterNames.CODE_VERIFIER)
                || !parameters.containsKey(OAuth2ParameterNames.CODE)
                || !parameters.containsKey(OAuth2ParameterNames.GRANT_TYPE)) {
            // not a valid request
            return null;
        }

        // PKCE is supported for auth_code only
        AuthorizationGrantType grantType = AuthorizationGrantType
                .parse(request.getParameter(OAuth2ParameterNames.GRANT_TYPE));

        if (AuthorizationGrantType.AUTHORIZATION_CODE != grantType) {
            return null;
        }

        // make sure we get exactly 1 value per parameter
        if (parameters.get(OAuth2ParameterNames.CLIENT_ID).length != 1
                || parameters.get(OAuth2ParameterNames.CODE).length != 1
                || parameters.get(PkceParameterNames.CODE_VERIFIER).length != 1) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        // read first (only) value from parameters
        String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
        String code = request.getParameter(OAuth2ParameterNames.CODE);
        String codeVerifier = request.getParameter(PkceParameterNames.CODE_VERIFIER);

        // validate parameters are *not* empty
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(codeVerifier)) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
        }

        // return our authRequest
        return new OAuth2ClientPKCEAuthenticationToken(clientId, code, codeVerifier,
                AuthenticationMethod.NONE.getValue());

    }

}
