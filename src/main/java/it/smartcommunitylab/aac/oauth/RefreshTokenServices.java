package it.smartcommunitylab.aac.oauth;

import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public interface RefreshTokenServices {

    /*
     * Create a refresh token associated with the given authentication
     */
    public OAuth2RefreshToken createRefreshToken(OAuth2Authentication authentication);

}
