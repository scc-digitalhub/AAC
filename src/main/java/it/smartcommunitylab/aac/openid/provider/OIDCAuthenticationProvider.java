package it.smartcommunitylab.aac.openid.provider;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticationToken;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

public class OIDCAuthenticationProvider extends ExtendedAuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OIDCUserAccountRepository accountRepository;
    private final OIDCIdentityProviderConfig providerConfig;

    private final OidcAuthorizationCodeAuthenticationProvider oidcProvider;
    private final OAuth2LoginAuthenticationProvider oauthProvider;

    public OIDCAuthenticationProvider(String providerId,
            OIDCUserAccountRepository accountRepository,
            OIDCIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_OIDC, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.providerConfig = config;
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
    public Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        // extract registrationId and check if matches our providerId
        OAuth2LoginAuthenticationToken loginAuthenticationToken = (OAuth2LoginAuthenticationToken) authentication;
        String registrationId = loginAuthenticationToken.getClientRegistration().getRegistrationId();
        if (!getProvider().equals(registrationId)) {
            // this login is not for us, let others process it
            return null;
        }

        // delegate to oauth providers in sequence
        Authentication auth = oidcProvider.authenticate(authentication);
        if (auth == null) {
            auth = oauthProvider.authenticate(authentication);
        }

        if (auth != null) {
            // convert to out authToken and clear exchange information, those are not
            // serializable..
            OAuth2LoginAuthenticationToken authenticationToken = (OAuth2LoginAuthenticationToken) auth;
            //
            auth = new OIDCAuthenticationToken(
                    authenticationToken.getPrincipal(),
                    authenticationToken.getAccessToken(),
                    authenticationToken.getRefreshToken(),
                    Collections.singleton(new SimpleGrantedAuthority(Config.R_USER)));
        }

        return auth;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication != null && OAuth2LoginAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    protected UserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        // we need to unpack user and fetch properties from repo
        OAuth2User oauthDetails = (OAuth2User) principal;

        // name is always available, is mapped via provider configuration
        String username = oauthDetails.getName();
        // prefer upstream subject identifier as userId where available
        String userId = StringUtils.hasText(oauthDetails.getAttribute("sub")) ? oauthDetails.getAttribute("sub")
                : oauthDetails.getName();

        // rebuild details to clear authorities
        // by default they contain the response body, ie. the full accessToken +
        // everything else

        // bind principal to ourselves
        // TODO use internal id for principal, not exported. Needs alignment in
        // accountprovider
        OIDCAuthenticatedPrincipal user = new OIDCAuthenticatedPrincipal(getProvider(), getRealm(),
                exportInternalId(userId));
        user.setName(username);
        user.setPrincipal(oauthDetails);

        return user;
    }

    @Override
    protected Instant expiresAt(Authentication auth) {
        // if enabled bind session duration to token expiration
        if (Boolean.TRUE.equals(providerConfig.getConfigMap().getRespectTokenExpiration())) {
            // build expiration from tokens
            OIDCAuthenticationToken token = (OIDCAuthenticationToken) auth;
            OAuth2User user = token.getPrincipal();
            if (user instanceof OidcUser) {
                // check for id token
                Instant exp = ((OidcUser) user).getExpiresAt();
                if (exp != null) {
                    return exp;
                }
            }

            OAuth2AccessToken accessToken = token.getAccessToken();
            if (accessToken != null) {
                return accessToken.getExpiresAt();
            }
        }

        return null;

    }

    private final GrantedAuthoritiesMapper nullAuthoritiesMapper = (authorities -> Collections.emptyList());

}
