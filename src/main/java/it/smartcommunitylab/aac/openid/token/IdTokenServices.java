package it.smartcommunitylab.aac.openid.token;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import it.smartcommunitylab.aac.openid.common.IdToken;

public interface IdTokenServices {

    /*
     * Create idToken
     */
    public IdToken createIdToken(OAuth2Authentication authentication) throws AuthenticationException;

    public IdToken createIdToken(OAuth2Authentication authentication, OAuth2AccessToken accessToken)
            throws AuthenticationException;

    public IdToken createIdToken(OAuth2Authentication authentication, String code)
            throws AuthenticationException;

    public IdToken createIdToken(OAuth2Authentication authentication, OAuth2AccessToken accessToken, String code)
            throws AuthenticationException;

}
