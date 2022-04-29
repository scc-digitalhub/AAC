package it.smartcommunitylab.aac.openid.apple.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AppleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private static ObjectMapper mapper = new ObjectMapper();

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        Assert.notNull(userRequest, "userRequest cannot be null");
        Assert.notNull(userRequest.getIdToken(), "id token is required");

        OidcUserInfo userInfo = null;

        // check if user obj is provided and insert as userinfo
        if (userRequest.getAdditionalParameters().containsKey("user")) {
            try {
                // map to user
                AppleOidcUserData userData = mapper.convertValue(userRequest.getAdditionalParameters().get("user"),
                        AppleOidcUserData.class);

                Map<String, Object> claims = new HashMap<>();
                claims.put("email", userData.email);
                claims.put("firstName", userData.name.firstName);
                claims.put("lastName", userData.name.lastName);
                userInfo = new OidcUserInfo(claims);

            } catch (Exception e) {
                // skip invalid data
            }
        }
        // build a default user from id token without authorities
        Set<GrantedAuthority> authorities = Collections.emptySet();
        return new DefaultOidcUser(authorities, userRequest.getIdToken(), userInfo);
    }

    private class AppleOidcUserData {
        public String email;
        public AppleOidcUserDataName name;
    }

    public class AppleOidcUserDataName {
        public String firstName;
        public String lastName;
    }
}
