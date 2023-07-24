package it.smartcommunitylab.aac.oauth.provider;

import org.springframework.security.oauth2.provider.OAuth2Authentication;

public interface PeekableAuthorizationCodeServices {
    OAuth2Authentication peekAuthorizationCode(String code);
}
