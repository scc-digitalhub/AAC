package it.smartcommunitylab.aac.openid.provider;

import java.time.Instant;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.model.UserStatus;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticationException;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticationToken;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountId;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

public class OIDCAuthenticationProvider extends ExtendedAuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OIDCUserAccountRepository accountRepository;
    private final OIDCIdentityProviderConfig config;

    private final OidcAuthorizationCodeAuthenticationProvider oidcProvider;
    private final OAuth2LoginAuthenticationProvider oauthProvider;

    public OIDCAuthenticationProvider(String providerId,
            OIDCUserAccountRepository accountRepository,
            OIDCIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountRepository, config, realm);
    }

    public OIDCAuthenticationProvider(
            String authority, String providerId,
            OIDCUserAccountRepository accountRepository,
            OIDCIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.config = config;
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

        // TODO extract codeResponse + tokenResponse for audit
        String authorizationRequest = loginAuthenticationToken.getAuthorizationExchange().getAuthorizationRequest()
                .getAuthorizationRequestUri();
        String authorizationResponse = loginAuthenticationToken.getAuthorizationExchange().getAuthorizationResponse()
                .getRedirectUri();

        // delegate to oauth providers in sequence
        try {
            Authentication auth = oidcProvider.authenticate(authentication);
            if (auth == null) {
                auth = oauthProvider.authenticate(authentication);
            }

            if (auth != null) {
                // convert to out authToken and clear exchange information, those are not
                // serializable..
                OAuth2LoginAuthenticationToken authenticationToken = (OAuth2LoginAuthenticationToken) auth;
                // extract sub identifier
                String subject = authenticationToken.getPrincipal().getAttribute(IdTokenClaimNames.SUB);
                if (!StringUtils.hasText(subject)) {
                    throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request"));
                }

                // check if account is present and locked
                OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(getProvider(), subject));
                if (account != null && !UserStatus.ACTIVE.getValue().equals(account.getStatus())) {
                    throw new OIDCAuthenticationException(new OAuth2Error("invalid_request"), "account not available",
                            authorizationRequest,
                            authorizationResponse, null, null);
                }

                auth = new OIDCAuthenticationToken(
                        subject,
                        authenticationToken.getPrincipal(),
                        authenticationToken.getAccessToken(),
                        authenticationToken.getRefreshToken(),
                        Collections.singleton(new SimpleGrantedAuthority(Config.R_USER)));
            }

            return auth;
        } catch (OAuth2AuthenticationException e) {
            throw new OIDCAuthenticationException(e.getError(), e.getMessage(), authorizationRequest,
                    authorizationResponse, null, null);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication != null && OAuth2LoginAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    protected OIDCUserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        // we need to unpack user and fetch properties from repo
        OAuth2User oauthDetails = (OAuth2User) principal;

        // upstream subject identifier
        String subject = oauthDetails.getAttribute(IdTokenClaimNames.SUB);

        // name is always available, is mapped via provider configuration
        String name = oauthDetails.getName();

        // we still don't have userId
        String userId = null;

        // rebuild details to clear authorities
        // by default they contain the response body, ie. the full accessToken +
        // everything else

        // bind principal to ourselves
        OIDCUserAuthenticatedPrincipal user = new OIDCUserAuthenticatedPrincipal(getAuthority(), getProvider(),
                getRealm(),
                userId, subject);
        user.setName(name);
        user.setPrincipal(oauthDetails);

        return user;
    }

    @Override
    protected Instant expiresAt(Authentication auth) {
        // if enabled bind session duration to token expiration
        if (Boolean.TRUE.equals(config.getConfigMap().getRespectTokenExpiration())) {
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
