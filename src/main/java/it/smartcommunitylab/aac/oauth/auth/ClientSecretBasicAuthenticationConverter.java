package it.smartcommunitylab.aac.oauth.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

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
        byte[] decodedToken;
        try {
            decodedToken = Base64.getDecoder().decode(base64Token);
        } catch (IllegalArgumentException e) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST), e);
        }

        // decode credentials
        String token = new String(decodedToken, StandardCharsets.UTF_8);
        String[] credentials = token.split(":", 2);
        if (credentials.length != 2) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        // NOTE: as per https://www.rfc-editor.org/rfc/rfc6749#section-2.3.1
        // both clientId and secret are urlencoded
        String clientId;
        String clientSecret;
        try {
            clientId = URLDecoder.decode(credentials[0], StandardCharsets.UTF_8.name());
            clientSecret = URLDecoder.decode(credentials[1], StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST), e);
        }

        // validate both clientId and secret are *not* empty
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            // throw oauth2 exception
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
        }

        // return our authRequest
        return new OAuth2ClientSecretAuthenticationToken(clientId, clientSecret,
                AuthenticationMethod.CLIENT_SECRET_BASIC.getValue());

    }

}
