package it.smartcommunitylab.aac.openid.provider;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.OIDCAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

public class OIDCAuthenticationProvider extends ExtendedAuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OIDCUserAccountRepository accountRepository;

    private final OidcAuthorizationCodeAuthenticationProvider oidcProvider;
    private final OAuth2LoginAuthenticationProvider oauthProvider;

    public OIDCAuthenticationProvider(String providerId,
            OIDCUserAccountRepository accountRepository,
            String realm) {
        super(SystemKeys.AUTHORITY_OIDC, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        this.accountRepository = accountRepository;

        // we support only authCode login
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();

        // we don't use the account repository to fetch user details,
        // use oidc userinfo to provide user details
        // TODO add jwt handling from id_token or access token
        this.oidcProvider = new OidcAuthorizationCodeAuthenticationProvider(accessTokenResponseClient,
                new OidcUserService());
        // oauth userinfo comes from oidc userinfo..
        this.oauthProvider = new OAuth2LoginAuthenticationProvider(accessTokenResponseClient,
                new DefaultOAuth2UserService());

        // use a custom authorities mapper to cleanup authorities spring injects
        // default impl translates the whole oauth response as an authority..
        this.oidcProvider.setAuthoritiesMapper(nullAuthoritiesMapper);
        this.oauthProvider.setAuthoritiesMapper(nullAuthoritiesMapper);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // TODO extract registrationId and check if matches our providerid
        //
        // delegate to oauth providers in sequence
        Authentication auth = oidcProvider.authenticate(authentication);
        if (auth == null) {
            auth = oauthProvider.authenticate(authentication);
        }

        // TODO
        // wrap provider token with custom class implementing eraseCredentials
        // also evaluate cleanup authentication token from details?
        if (auth != null) {
            OAuth2LoginAuthenticationToken authenticationToken = (OAuth2LoginAuthenticationToken) auth;

        }

        return auth;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2LoginAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    protected UserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        // we need to unpack user and fetch properties from repo
        OAuth2User oauthDetails = (OAuth2User) principal;

        // TODO complete mapping, for now this suffices
        String userId = oauthDetails.getName();
        String username = StringUtils.hasText(oauthDetails.getAttribute("email")) ? oauthDetails.getAttribute("email")
                : userId;

        // rebuild details to clear authorities
        // by default they contain the response body, ie. the full accessToken +
        // everything else

        // bind principal to ourselves
        OIDCAuthenticatedPrincipal user = new OIDCAuthenticatedPrincipal(getProvider(), getRealm(),
                exportInternalId(userId));
        user.setName(username);
        user.setPrincipal(oauthDetails);

        return user;
    }

    private final GrantedAuthoritiesMapper nullAuthoritiesMapper = (authorities -> Collections.emptyList());

}
