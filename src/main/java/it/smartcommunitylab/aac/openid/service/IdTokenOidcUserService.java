package it.smartcommunitylab.aac.openid.service;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class IdTokenOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    // define claim converter for oidc standard values
    private static final Converter<Map<String, Object>, Map<String, Object>> claimConverter = new ClaimTypeConverter(
        OidcUserService.createDefaultClaimTypeConverters()
    );

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        Assert.notNull(userRequest, "userRequest cannot be null");
        Assert.notNull(userRequest.getIdToken(), "id token is required");

        // get id_token and extract claims
        OidcIdToken idToken = userRequest.getIdToken();

        // leverage default converters to translate
        Map<String, Object> claims = claimConverter.convert(idToken.getClaims());
        OidcUserInfo userInfo = new OidcUserInfo(claims);

        // subject MUST be defined
        if (userInfo.getSubject() == null) {
            OAuth2Error oauth2Error = new OAuth2Error("invalid_user_info_response");
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }

        // build default user with standard authorities
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new OidcUserAuthority(userRequest.getIdToken(), userInfo));
        OAuth2AccessToken token = userRequest.getAccessToken();
        for (String authority : token.getScopes()) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + authority));
        }

        // check if userName attribute is defined, or fallback to sub
        ProviderDetails providerDetails = userRequest.getClientRegistration().getProviderDetails();
        String userNameAttributeName = providerDetails.getUserInfoEndpoint().getUserNameAttributeName();
        if (StringUtils.hasText(userNameAttributeName)) {
            return new DefaultOidcUser(authorities, userRequest.getIdToken(), userInfo, userNameAttributeName);
        }
        return new DefaultOidcUser(authorities, userRequest.getIdToken(), userInfo);
    }
}
