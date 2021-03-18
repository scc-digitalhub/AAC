package it.smartcommunitylab.aac.oauth;

import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public interface PeekableAuthorizationCodeServices {

    OAuth2Authentication peekAuthorizationCode(String code);
}
