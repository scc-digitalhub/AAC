package it.smartcommunitylab.aac.oauth.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;

public class ClientSecretBasicAuthenticationConverter extends OAuth2ClientAuthenticationConverter {
    public static final String AUTHORIZATION_HEADER_BASIC = "Basic";
    public static final String AUTHORIZATION_HEADER_SEPARATOR = ":";

    @Override
    public OAuth2ClientSecretAuthenticationToken attemptConvert(HttpServletRequest request) {
        try {
            Pair<String, Optional<String>> basicAuth = extractBasicAuth(request);
            if (basicAuth == null) {
                throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
            }

            String clientId = basicAuth.getFirst();
            String clientSecret = basicAuth.getSecond().orElse(null);

            // validate both clientId and secret are *not* empty
            if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
                // throw oauth2 exception
                throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
            }

            // return our authRequest
            return new OAuth2ClientSecretAuthenticationToken(clientId, clientSecret,
                    AuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST), e);
        }

    }

    public static Pair<String, Optional<String>> extractBasicAuth(HttpServletRequest request)
            throws IllegalArgumentException, UnsupportedEncodingException {
        // read from header
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) {
            return null;
        }
        header = header.trim();
        if (!StringUtils.startsWithIgnoreCase(header, AUTHORIZATION_HEADER_BASIC)) {
            return null;
        }

        // get base64 and decode string
        byte[] base64Token = header.substring(6).getBytes(StandardCharsets.UTF_8);
        byte[] decodedToken = Base64.getDecoder().decode(base64Token);

        // decode credentials
        String token = new String(decodedToken, StandardCharsets.UTF_8);
        String[] credentials = token.split(":", 2);
        if (credentials.length != 1 && credentials.length != 2) {
            return null;
        }

        // NOTE: as per https://www.rfc-editor.org/rfc/rfc6749#section-2.3.1
        // both clientId and secret are urlencoded
        String clientId = URLDecoder.decode(credentials[0], StandardCharsets.UTF_8.name());

        // secret is optional
        String clientSecret = null;
        if (credentials.length == 2) {
            clientSecret = URLDecoder.decode(credentials[1], StandardCharsets.UTF_8.name());
        }

        return Pair.of(clientId, Optional.ofNullable(clientSecret));
    }

}
