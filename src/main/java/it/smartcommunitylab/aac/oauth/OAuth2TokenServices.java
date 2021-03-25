package it.smartcommunitylab.aac.oauth;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.approval.Approval.ApprovalStatus;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ParseException;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;

/*
 * A complete tokenServices implementation.
 * 
 * Each request will result in a new token
 */

public class OAuth2TokenServices implements AuthorizationServerTokenServices, ConsumerTokenServices,
        ResourceServerTokenServices, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // TODO remove logger in favor of events
    private static final Logger traceUserLogger = LoggerFactory.getLogger("traceUserToken");

    // static config
    private static final BytesKeyGenerator TOKEN_GENERATOR = KeyGenerators.secureRandom(20);
    private static final Charset ENCODE_CHARSET = Charset.forName("US-ASCII");

    // default token duration
    public static final int DEFAULT_ACCESS_TOKEN_VALIDITY = 60 * 60 * 6; // 6 hours
    public static final int DEFAULT_REFRESH_TOKEN_VALIDITY = 60 * 60 * 24 * 30; // 30 days
    public static final int DEFAULT_REFRESH_TOKEN_RENEWAL_WINDOW = 60 * 60 * 24 * 3; // 3 days

    // services
    private final ExtTokenStore tokenStore;
    private ApprovalStore approvalStore;

    private AACTokenEnhancer tokenEnhancer;
    private OAuth2ClientDetailsService clientDetailsService;

    // configurable properties
    private BytesKeyGenerator tokenGenerator;
    private int refreshTokenValiditySeconds;
    private int accessTokenValiditySeconds;
    private int refreshTokenRenewalWindowSeconds;
    private boolean removeExpired = true;

    private Object refreshLock = new Object();

    // TODO implement a refresh for stale user authentication on refreshtokens
    // at minimum we need to validate user existence, but we should really recover
    // identities from scratch and rebuild UserDetails
    // otherwise we will keep serving the same claims
    // needs a new authToken == PreAuthenticatedAuthenticationToken and the relative
    // authprovider
//    private boolean refreshAuthentication = false;
//  private final ExtendedAuthenticationManager authManager;

    public OAuth2TokenServices(ExtTokenStore tokenStore) {
        Assert.notNull(tokenStore, "tokenStore is mandatory");
        this.tokenStore = tokenStore;
        this.tokenGenerator = TOKEN_GENERATOR;
        this.refreshTokenValiditySeconds = DEFAULT_REFRESH_TOKEN_VALIDITY;
        this.accessTokenValiditySeconds = DEFAULT_ACCESS_TOKEN_VALIDITY;
        this.refreshTokenRenewalWindowSeconds = DEFAULT_REFRESH_TOKEN_RENEWAL_WINDOW;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(tokenStore, "token store is mandatory");
        Assert.notNull(clientDetailsService, "client details service is mandatory");

    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OAuth2AccessToken createAccessToken(OAuth2Authentication authentication) throws AuthenticationException {
        logger.debug("create access token for authentication " + authentication.getName());
        OAuth2Request request = authentication.getOAuth2Request();
        String clientId = request.getClientId();

        // fetch client
        OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

        // validity interval for tokens
        int accessValiditySeconds = clientDetails.getAccessTokenValiditySeconds() != null
                ? clientDetails.getAccessTokenValiditySeconds()
                : accessTokenValiditySeconds;
        int refreshValiditySeconds = clientDetails.getRefreshTokenValiditySeconds() != null
                ? clientDetails.getRefreshTokenValiditySeconds()
                : refreshTokenValiditySeconds;

        AACOAuth2AccessToken accessToken = createAccessToken(authentication, accessValiditySeconds);
        if (accessToken == null || !StringUtils.hasText(accessToken.getValue())) {
            throw new OAuth2Exception("token error");
        }

        // if supported generate a new refresh token
        if (supportsRefreshToken(authentication)) {
            OAuth2RefreshToken refreshToken = createRefreshToken(authentication, refreshValiditySeconds);
            if (refreshToken != null && StringUtils.hasText(refreshToken.getValue())) {
                tokenStore.storeRefreshToken(refreshToken, authentication);
                accessToken.setRefreshToken(refreshToken);
            }
        }

        // call enhancer
        if (tokenEnhancer != null) {
            accessToken = tokenEnhancer.enhance(accessToken, authentication);
        }

        tokenStore.storeAccessToken(accessToken, authentication);

        traceUserLogger.info(String.format("'type':'new','user':'%s','scope':'%s','token':'%s'",
                authentication.getName(), String.join(" ", accessToken.getScope()), accessToken.getValue()));
        return accessToken;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OAuth2AccessToken refreshAccessToken(String refreshTokenValue, TokenRequest tokenRequest)
            throws AuthenticationException {
        logger.debug("refresh access token for token " + refreshTokenValue);

        OAuth2RefreshToken refreshToken = tokenStore.readRefreshToken(refreshTokenValue);
        if (refreshToken == null) {
            throw new InvalidGrantException("Invalid refresh token: " + refreshTokenValue);
        }

        // fetch authentication
        // TODO build a new preauthented request and refresh identities
        OAuth2Authentication authentication = tokenStore.readAuthenticationForRefreshToken(refreshToken);

        // check if userAuth is present
        Authentication userAuth = authentication.getUserAuthentication();
        if (userAuth == null || !(userAuth instanceof UserAuthenticationToken)) {
            throw new InvalidRequestException("refresh requires a valid user authentication");
        }

        // validate now if client is the same as the authorized one
        String clientId = authentication.getOAuth2Request().getClientId();
        if (clientId == null || !clientId.equals(tokenRequest.getClientId())) {
            throw new InvalidGrantException("Wrong client for this refresh token: " + refreshTokenValue);
        }

        // fetch client
        OAuth2ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

        // validity interval for tokens
        int accessValiditySeconds = clientDetails.getAccessTokenValiditySeconds() != null
                ? clientDetails.getAccessTokenValiditySeconds()
                : accessTokenValiditySeconds;
        int refreshValiditySeconds = clientDetails.getRefreshTokenValiditySeconds() != null
                ? clientDetails.getRefreshTokenValiditySeconds()
                : refreshTokenValiditySeconds;

        // pick renewal to contain at least a number of access tokens
        int refreshRenewalSeconds = Math.max(refreshTokenRenewalWindowSeconds, accessValiditySeconds * 12);

        // TODO evaluate refusing refresh with not-authorized scopes: now we simply
        // avoid those and return only those authorized, we could return an error and
        // avoid removing other tokens

        // lock to make this call atomic, otherwise we could concurrently delete the
        // newly created access tokens
        // TODO rework with a keyed lock (per client/per user?) to improve performance
        // also note that AuthorizationEndpoint has a similar approach
        AACOAuth2AccessToken accessToken;

        synchronized (this.refreshLock) {
            // remove old access tokens, we enforce a single refresh -> accessToken
            // this way clients will be able to invalidate old tokens by asking refresh
            // for the same reason we build each time a new accessToken
            tokenStore.removeAccessTokenUsingRefreshToken(refreshToken);

            boolean renewToken = false;
            if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
                // check if expired
                ExpiringOAuth2RefreshToken expiringToken = (ExpiringOAuth2RefreshToken) refreshToken;
                boolean isExpired = (expiringToken.getExpiration() == null
                        || System.currentTimeMillis() > expiringToken.getExpiration().getTime());

                if (isExpired) {
                    tokenStore.removeRefreshToken(refreshToken);
                    throw new InvalidTokenException("Invalid refresh token (expired): " + refreshToken);
                }

                // renew if within window
                renewToken = (!isExpired
                        && System.currentTimeMillis() > (expiringToken.getExpiration().getTime()
                                - refreshRenewalSeconds));

            }

            // build a new oauthAuthentication matching tokenRequest
            OAuth2Authentication refreshedAuthentication = refreshAuthentication(authentication, tokenRequest);

            // build a new accessToken
            logger.debug("create access token for authentication " + refreshedAuthentication.getName());

            accessToken = createAccessToken(refreshedAuthentication, accessValiditySeconds);
            if (accessToken == null || !StringUtils.hasText(accessToken.getValue())) {
                throw new OAuth2Exception("token error");
            }

            // make sure we return the same refresh token
            accessToken.setRefreshToken(refreshToken);

            // if needed build a new refresh token and replace in response
            if (renewToken) {
                // if we renew use the original authentication, not the refreshed
                OAuth2RefreshToken refreshedToken = createRefreshToken(authentication, refreshValiditySeconds);
                if (refreshedToken != null && StringUtils.hasText(refreshedToken.getValue())) {
                    tokenStore.removeRefreshToken(refreshToken);
                    tokenStore.storeRefreshToken(refreshedToken, authentication);
                    accessToken.setRefreshToken(refreshedToken);
                }
            }

            // call enhancer
            if (tokenEnhancer != null) {
                accessToken = tokenEnhancer.enhance(accessToken, refreshedAuthentication);
            }

            tokenStore.storeAccessToken(accessToken, refreshedAuthentication);
        }

        traceUserLogger.info(String.format("'type':'new','user':'%s','scope':'%s','token':'%s'",
                authentication.getName(), String.join(" ", accessToken.getScope()), accessToken.getValue()));
        return accessToken;

    }

//    @Override
//    @Transactional(isolation = Isolation.SERIALIZABLE)
//    public OAuth2RefreshToken createRefreshToken(OAuth2Authentication authentication) {
//
//        // TODO enforce validation of "offline_access" scope for refresh tokens?
//
//        // TODO Auto-generated method stub
//        return null;
//    }

    @Override
    public boolean revokeToken(String tokenValue) {

        // we don't know if the value identifies an access or refresh token
        // note that we could theoretically find collisions...
        boolean removedAccessToken = false;
        boolean removedRefreshToken = false;

        // fetch an access token
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(tokenValue);
        if (accessToken != null) {
            // remove refreshToken if associated
            if (accessToken.getRefreshToken() != null) {
                tokenStore.removeRefreshToken(accessToken.getRefreshToken());
            }
            tokenStore.removeAccessToken(accessToken);

            removedAccessToken = true;
        }

        OAuth2RefreshToken refreshToken = tokenStore.readRefreshToken(tokenValue);
        if (refreshToken != null) {
            // we need to revoke all access tokens first
            tokenStore.removeAccessTokenUsingRefreshToken(refreshToken);
            tokenStore.removeRefreshToken(refreshToken);

            removedRefreshToken = true;

        }

        return removedAccessToken || removedRefreshToken;
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessTokenValue) throws AuthenticationException,
            InvalidTokenException {

        // load token and if needed remove expired
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(accessTokenValue);
        if (accessToken == null) {
            throw new InvalidTokenException("Invalid access token: " + accessTokenValue);
        } else if (accessToken.isExpired()) {

            if (removeExpired) {
                tokenStore.removeAccessToken(accessToken);
            }

            throw new InvalidTokenException("Access token expired: " + accessTokenValue);
        }

        OAuth2Authentication authentication = tokenStore.readAuthentication(accessToken);
        // validate client
        String clientId = authentication.getOAuth2Request().getClientId();

        try {
            clientDetailsService.loadClientByClientId(clientId);
        } catch (ClientRegistrationException e) {
            throw new InvalidTokenException("Client not valid: " + clientId, e);
        }

        return authentication;
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessTokenValue) {
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(accessTokenValue);
        if (accessToken == null) {
            throw new InvalidTokenException("Invalid access token: " + accessTokenValue);
        }

        return accessToken;
    }

    @Override
    public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
        // we don't want to read tokens from auth requests, it is really a "you need to
        // know the value (or key)" situation. Besides our tokenStore implementation
        // will simply return null
        return null;
    }

    private ExpiringOAuth2RefreshToken createRefreshToken(OAuth2Authentication authentication, int validitySeconds) {
        OAuth2Request request = authentication.getOAuth2Request();
        String clientId = request.getClientId();
        Set<String> scopes = request.getScope();

        if (!scopes.contains(Config.SCOPE_OFFLINE_ACCESS)) {
            logger.error("client " + clientId + " requested a refresh token without offline_access scope");
        }

        logger.trace("create refresh token for " + clientId + " with validity "
                + String.valueOf(validitySeconds));

        // use a secure string as value to respect requirement
        // https://tools.ietf.org/html/rfc6749#section-10.10
        // 160bit = a buffer of 20 random bytes
        String value = new String(Base64.encodeBase64URLSafe(tokenGenerator.generateKey()), ENCODE_CHARSET);

        ExpiringOAuth2RefreshToken refreshToken = new DefaultExpiringOAuth2RefreshToken(value,
                new Date(System.currentTimeMillis() + (validitySeconds * 1000L)));
        return refreshToken;
    }

    private AACOAuth2AccessToken createAccessToken(OAuth2Authentication authentication, int validitySeconds) {

        OAuth2Request request = authentication.getOAuth2Request();
        String clientId = request.getClientId();
        Set<String> scopes = request.getScope();

        logger.trace("create access token for " + clientId + " with validity "
                + String.valueOf(validitySeconds));

        // use a secure string as value to respect requirement
        // https://tools.ietf.org/html/rfc6749#section-10.10
        // 160bit = a buffer of 20 random bytes
        String value = new String(Base64.encodeBase64URLSafe(tokenGenerator.generateKey()), ENCODE_CHARSET);
        AACOAuth2AccessToken token = new AACOAuth2AccessToken(value);

        token.setExpiration(new Date(System.currentTimeMillis() + (validitySeconds * 1000L)));
        token.setScope(scopes);

        logger.info("Created token " + token.getValue() + " expires at " + token.getExpiration());
        return token;
    }

    private OAuth2Authentication refreshAuthentication(OAuth2Authentication authentication, TokenRequest tokenRequest) {

        String clientId = tokenRequest.getClientId();

        // check here if requested scopes are subset of granted
        Set<String> scopes = tokenRequest.getScope();
        Set<String> authorizedScopes = authentication.getOAuth2Request().getScope();
        // we should also refresh scopes from approvalStore and check that the
        // client can still use the scopes previously authorized
        Set<String> approvedScopes = authorizedScopes;
        if (approvalStore != null) {
            approvedScopes = getUserApproved(authentication.getUserAuthentication().getName(), clientId);
        }

        // only scopes still approved are allowed
        Set<String> allowedScopes = new HashSet<>();
        for (String scope : authorizedScopes) {
            if (approvedScopes.contains(scope)) {
                allowedScopes.add(scope);
            }
        }

        OAuth2Request request = authentication.getOAuth2Request().refresh(tokenRequest);

        // we narrow down if scopes are specified, otherwise we'll keep those authorized
        if (scopes != null && !scopes.isEmpty()) {
            // narrow down authorized scopes to requested
            // we filter here since narrowScope on request really is a setScope..
            Set<String> narrowedScope = scopes.stream().filter(s -> allowedScopes.contains(s))
                    .collect(Collectors.toSet());
            request = request.narrowScope(narrowedScope);
        }

        return new OAuth2Authentication(request, authentication.getUserAuthentication());
    }

    private boolean supportsRefreshToken(OAuth2Authentication authentication) {
        OAuth2Request request = authentication.getOAuth2Request();

        // validate grantType
        GrantType grantType = null;
        try {
            grantType = GrantType.parse(request.getGrantType());
        } catch (ParseException e) {
            // invalid grant type, should not happen here
        }

        if (!GrantType.AUTHORIZATION_CODE.equals(grantType)
                && !GrantType.PASSWORD.equals(grantType)
                && !GrantType.DEVICE_CODE.equals(grantType)) {
            return false;
        }

        // validate scope offline_access?
        // TODO only for some flows

        // validate userAuth, without there is no reason to release refresh tokens
        if (authentication.isClientOnly()) {
            return false;
        }

        return true;

    }

    private Set<String> getUserApproved(String subjectId, String clientId) {
        Set<String> userApprovedScopes = new HashSet<>();

        // fetch previously approved from store
        Collection<Approval> userApprovals = approvalStore.getApprovals(subjectId, clientId);
        Set<Approval> expiredApprovals = new HashSet<>();

        // add those not expired to list and remove others
        for (Approval approval : userApprovals) {
            if (approval.isCurrentlyActive()) {
                // check if approved or denied, we'll let user decide again on denied
                if (approval.getStatus().equals(ApprovalStatus.APPROVED)) {
                    userApprovedScopes.add(approval.getScope());
                }
            } else {
                // inactive means expired, cleanup
                expiredApprovals.add(approval);
            }
        }

        // cleanup expired
        if (!expiredApprovals.isEmpty()) {
            approvalStore.revokeApprovals(expiredApprovals);
        }
        return userApprovedScopes;
    }

    /*
     * Properties
     */
    public void setTokenEnhancer(AACTokenEnhancer accessTokenEnhancer) {
        this.tokenEnhancer = accessTokenEnhancer;
    }

    public void setClientDetailsService(OAuth2ClientDetailsService clientDetailsService) {
        this.clientDetailsService = clientDetailsService;
    }

    public void setRefreshTokenValiditySeconds(int refreshTokenValiditySeconds) {
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    public void setAccessTokenValiditySeconds(int accessTokenValiditySeconds) {
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public void setRefreshTokenRenewalWindowSeconds(int refreshTokenRenewalWindowSeconds) {
        this.refreshTokenRenewalWindowSeconds = refreshTokenRenewalWindowSeconds;
    }

    public void setTokenGenerator(BytesKeyGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    public void setRemoveExpired(boolean removeExpired) {
        this.removeExpired = removeExpired;
    }

    public void setApprovalStore(ApprovalStore approvalStore) {
        this.approvalStore = approvalStore;
    }

}
