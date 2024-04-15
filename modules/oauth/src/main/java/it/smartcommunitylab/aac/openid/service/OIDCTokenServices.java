/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.openid.service;

import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.Claim;
import it.smartcommunitylab.aac.claims.ClaimsSet;
import it.smartcommunitylab.aac.clients.service.ClientDetailsService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.jwt.JWTService;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.common.SecureStringKeyGenerator;
import it.smartcommunitylab.aac.oauth.common.ServerErrorException;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.openid.common.IdToken;
import it.smartcommunitylab.aac.openid.token.IdTokenServices;
import it.smartcommunitylab.aac.profiles.claims.OpenIdClaimsExtractorProvider;
import it.smartcommunitylab.aac.profiles.scope.OpenIdAddressScope;
import it.smartcommunitylab.aac.profiles.scope.OpenIdDefaultScope;
import it.smartcommunitylab.aac.profiles.scope.OpenIdEmailScope;
import it.smartcommunitylab.aac.profiles.scope.OpenIdPhoneScope;
import it.smartcommunitylab.aac.users.auth.UserAuthentication;
import it.smartcommunitylab.aac.users.model.User;
import it.smartcommunitylab.aac.users.model.UserDetails;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OIDCTokenServices implements IdTokenServices, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String MAX_AGE = "max_age";
    private static final String NONCE = "nonce";
    private static final Set<String> OPENID_SCOPES;

    // static config
    public static final int DEFAULT_ID_TOKEN_VALIDITY = 60 * 60; // 1 hour
    private static final StringKeyGenerator TOKEN_GENERATOR = new SecureStringKeyGenerator(20);
    private static final Charset ENCODE_CHARSET = Charset.forName("US-ASCII");

    static {
        Set<String> s = new HashSet<>();
        s.add(OpenIdEmailScope.SCOPE);
        s.add(OpenIdDefaultScope.SCOPE);
        s.add(OpenIdPhoneScope.SCOPE);
        s.add(OpenIdAddressScope.SCOPE);

        OPENID_SCOPES = Collections.unmodifiableSet(s);
    }

    private final String issuer;
    private final OpenIdClaimsExtractorProvider claimsExtractorProvider;
    private final JWTService jwtService;

    private int idTokenValiditySeconds;

    // TODO remove generic client service, use only oauth2
    private ClientDetailsService clientService;
    private OAuth2ClientDetailsService clientDetailsService;
    private StringKeyGenerator tokenGenerator;

    public OIDCTokenServices(
        String issuer,
        OpenIdClaimsExtractorProvider claimsExtractorProvider,
        JWTService jwtService
    ) {
        Assert.hasText(issuer, "issuer can not be null or empty");
        Assert.notNull(claimsExtractorProvider, "openid claims extractor is required");
        Assert.notNull(jwtService, "jwt service is required");

        this.issuer = issuer;
        this.claimsExtractorProvider = claimsExtractorProvider;
        this.jwtService = jwtService;
        this.tokenGenerator = TOKEN_GENERATOR;
        this.idTokenValiditySeconds = DEFAULT_ID_TOKEN_VALIDITY;
    }

    public void setClientService(ClientDetailsService clientService) {
        this.clientService = clientService;
    }

    public void setClientDetailsService(OAuth2ClientDetailsService clientDetailsService) {
        this.clientDetailsService = clientDetailsService;
    }

    public void setTokenGenerator(StringKeyGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    public void setIdTokenValiditySeconds(int idTokenValiditySeconds) {
        this.idTokenValiditySeconds = idTokenValiditySeconds;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(clientService, "client details service is required");
        Assert.notNull(clientDetailsService, "oauth2 client details service is required");
    }

    @Override
    public IdToken createIdToken(OAuth2Authentication authentication) throws AuthenticationException {
        if (authentication == null) {
            throw new InsufficientAuthenticationException("Invalid authentication");
        }

        // extract info
        OAuth2Request request = authentication.getOAuth2Request();
        String clientId = request.getClientId();
        Set<String> scopes = request.getScope();

        if (!scopes.contains(Config.SCOPE_OPENID)) {
            return null;
        }

        try {
            OAuth2ClientDetails oauth2ClientDetails = clientDetailsService.loadClientByClientId(clientId);

            Authentication userAuth = authentication.getUserAuthentication();
            if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
                throw new InvalidRequestException("id_token requires a valid user authentication");
            }

            UserDetails userDetails = ((UserAuthentication) userAuth).getUserDetails();
            User user = ((UserAuthentication) userAuth).getUser();

            String subject = userDetails.getUserId();

            // set single audience as string - correct for OIDC
            // audience could be a list but we don't want to add audience from accesstoken
            // TODO evaluate
            String[] audience = new String[] { clientId };

            // build jti via secure generator
            String jti = tokenGenerator.generateKey();

            Map<String, Object> claims = buildClaims(
                authentication,
                request,
                userDetails,
                user,
                oauth2ClientDetails,
                null,
                null
            );

            // dates
            Instant issuedAt = Instant.now();

            // evaluate expiration
            int validitySeconds = oauth2ClientDetails.getIdTokenValiditySeconds() != null
                ? oauth2ClientDetails.getIdTokenValiditySeconds()
                : idTokenValiditySeconds;
            Instant expiresAt = issuedAt.plusSeconds(validitySeconds);

            IdToken idToken = new IdToken(issuer, subject, audience, jti, issuedAt, expiresAt, issuedAt, claims);

            // build and sign, will encrypt if required by config
            JWT jwt = jwtService.buildAndSignJWT(oauth2ClientDetails, idToken.getClaimsSet());
            idToken.setValue(jwt.serialize());
            return idToken;
        } catch (AuthenticationException | OAuth2Exception e) {
            logger.error("oauth2 error: " + e.getMessage());
            throw e;
        } catch (NoSuchClientException | ClientRegistrationException e) {
            logger.error("non existing client: " + e.getMessage());
            throw new InvalidClientException("invalid client");
        } catch (RuntimeException | NoSuchResourceException | InvalidDefinitionException e) {
            logger.error("claims service error: " + e.getMessage());
            throw new OAuth2Exception(e.getMessage());
        }
    }

    @Override
    public IdToken createIdToken(OAuth2Authentication authentication, OAuth2AccessToken accessToken)
        throws AuthenticationException {
        if (authentication == null) {
            throw new InsufficientAuthenticationException("Invalid authentication");
        }

        if (accessToken == null) {
            throw new IllegalArgumentException("access token can not be null");
        }

        // extract info
        OAuth2Request request = authentication.getOAuth2Request();
        String clientId = request.getClientId();
        Set<String> scopes = request.getScope();

        if (!scopes.contains(Config.SCOPE_OPENID)) {
            return null;
        }

        try {
            OAuth2ClientDetails oauth2ClientDetails = clientDetailsService.loadClientByClientId(clientId);

            Authentication userAuth = authentication.getUserAuthentication();
            if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
                throw new InvalidRequestException("id_token requires a valid user authentication");
            }

            UserDetails userDetails = ((UserAuthentication) userAuth).getUserDetails();
            User user = ((UserAuthentication) userAuth).getUser();

            String subject = userDetails.getUserId();

            // set single audience as string - correct for OIDC
            // audience could be a list but we don't want to add audience from accesstoken
            // TODO evaluate
            String[] audience = new String[] { clientId };

            // build jti via secure generator
            String jti = tokenGenerator.generateKey();

            Map<String, Object> claims = buildClaims(
                authentication,
                request,
                userDetails,
                user,
                oauth2ClientDetails,
                accessToken,
                null
            );

            // dates
            Instant issuedAt = Instant.now();

            // evaluate expiration
            int validitySeconds = oauth2ClientDetails.getIdTokenValiditySeconds() != null
                ? oauth2ClientDetails.getIdTokenValiditySeconds()
                : idTokenValiditySeconds;
            Instant expiresAt = issuedAt.plusSeconds(validitySeconds);

            IdToken idToken = new IdToken(issuer, subject, audience, jti, issuedAt, expiresAt, issuedAt, claims);

            // build and sign, will encrypt if required by config
            JWT jwt = jwtService.buildAndSignJWT(oauth2ClientDetails, idToken.getClaimsSet());
            idToken.setValue(jwt.serialize());
            return idToken;
        } catch (AuthenticationException | OAuth2Exception e) {
            logger.error("oauth2 error: " + e.getMessage());
            throw e;
        } catch (NoSuchClientException | ClientRegistrationException e) {
            logger.error("non existing client: " + e.getMessage());
            throw new InvalidClientException("invalid client");
        } catch (RuntimeException | NoSuchResourceException | InvalidDefinitionException e) {
            logger.error("claims service error: " + e.getMessage());
            throw new OAuth2Exception(e.getMessage());
        }
    }

    @Override
    public IdToken createIdToken(OAuth2Authentication authentication, String code) throws AuthenticationException {
        if (authentication == null) {
            throw new InsufficientAuthenticationException("Invalid authentication");
        }

        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("code is required");
        }

        // extract info
        OAuth2Request request = authentication.getOAuth2Request();
        String clientId = request.getClientId();
        Set<String> scopes = request.getScope();

        if (!scopes.contains(Config.SCOPE_OPENID)) {
            return null;
        }

        try {
            OAuth2ClientDetails oauth2ClientDetails = clientDetailsService.loadClientByClientId(clientId);

            Authentication userAuth = authentication.getUserAuthentication();
            if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
                throw new InvalidRequestException("id_token requires a valid user authentication");
            }

            UserDetails userDetails = ((UserAuthentication) userAuth).getUserDetails();
            User user = ((UserAuthentication) userAuth).getUser();

            String subject = userDetails.getUserId();

            // set single audience as string - correct for OIDC
            // audience could be a list but we don't want to add audience from accesstoken
            // TODO evaluate
            String[] audience = new String[] { clientId };

            // build jti via secure generator
            String jti = tokenGenerator.generateKey();

            Map<String, Object> claims = buildClaims(
                authentication,
                request,
                userDetails,
                user,
                oauth2ClientDetails,
                null,
                code
            );

            // dates
            Instant issuedAt = Instant.now();

            // evaluate expiration
            int validitySeconds = oauth2ClientDetails.getIdTokenValiditySeconds() != null
                ? oauth2ClientDetails.getIdTokenValiditySeconds()
                : idTokenValiditySeconds;
            Instant expiresAt = issuedAt.plusSeconds(validitySeconds);

            IdToken idToken = new IdToken(issuer, subject, audience, jti, issuedAt, expiresAt, issuedAt, claims);

            // build and sign, will encrypt if required by config
            JWT jwt = jwtService.buildAndSignJWT(oauth2ClientDetails, idToken.getClaimsSet());
            idToken.setValue(jwt.serialize());
            return idToken;
        } catch (AuthenticationException | OAuth2Exception e) {
            logger.error("oauth2 error: " + e.getMessage());
            throw e;
        } catch (NoSuchClientException | ClientRegistrationException e) {
            logger.error("non existing client: " + e.getMessage());
            throw new InvalidClientException("invalid client");
        } catch (RuntimeException | NoSuchResourceException | InvalidDefinitionException e) {
            logger.error("claims service error: " + e.getMessage());
            throw new OAuth2Exception(e.getMessage());
        }
    }

    @Override
    public IdToken createIdToken(OAuth2Authentication authentication, OAuth2AccessToken accessToken, String code)
        throws AuthenticationException {
        if (authentication == null) {
            throw new InsufficientAuthenticationException("Invalid authentication");
        }

        if (accessToken == null) {
            throw new IllegalArgumentException("access token can not be null");
        }

        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("code is required");
        }

        // extract info
        OAuth2Request request = authentication.getOAuth2Request();
        String clientId = request.getClientId();
        Set<String> scopes = request.getScope();

        if (!scopes.contains(Config.SCOPE_OPENID)) {
            return null;
        }

        try {
            OAuth2ClientDetails oauth2ClientDetails = clientDetailsService.loadClientByClientId(clientId);

            Authentication userAuth = authentication.getUserAuthentication();
            if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
                throw new InvalidRequestException("id_token requires a valid user authentication");
            }

            UserDetails userDetails = ((UserAuthentication) userAuth).getUserDetails();
            User user = ((UserAuthentication) userAuth).getUser();

            String subject = userDetails.getUserId();

            // set single audience as string - correct for OIDC
            // audience could be a list but we don't want to add audience from accesstoken
            // TODO evaluate
            String[] audience = new String[] { clientId };

            // build jti via secure generator
            String jti = tokenGenerator.generateKey();

            Map<String, Object> claims = buildClaims(
                authentication,
                request,
                userDetails,
                user,
                oauth2ClientDetails,
                accessToken,
                code
            );

            // dates
            Instant issuedAt = Instant.now();

            // evaluate expiration
            int validitySeconds = oauth2ClientDetails.getIdTokenValiditySeconds() != null
                ? oauth2ClientDetails.getIdTokenValiditySeconds()
                : idTokenValiditySeconds;
            Instant expiresAt = issuedAt.plusSeconds(validitySeconds);

            IdToken idToken = new IdToken(issuer, subject, audience, jti, issuedAt, expiresAt, issuedAt, claims);

            // build and sign, will encrypt if required by config
            JWT jwt = jwtService.buildAndSignJWT(oauth2ClientDetails, idToken.getClaimsSet());
            idToken.setValue(jwt.serialize());
            return idToken;
        } catch (AuthenticationException | OAuth2Exception e) {
            logger.error("oauth2 error: " + e.getMessage());
            throw e;
        } catch (NoSuchClientException | ClientRegistrationException e) {
            logger.error("non existing client: " + e.getMessage());
            throw new InvalidClientException("invalid client");
        } catch (RuntimeException | NoSuchResourceException | InvalidDefinitionException e) {
            logger.error("claims service error: " + e.getMessage());
            throw new OAuth2Exception(e.getMessage());
        }
    }

    private Map<String, Object> buildClaims(
        OAuth2Authentication authentication,
        OAuth2Request request,
        UserDetails userDetails,
        User user,
        OAuth2ClientDetails oauth2ClientDetails,
        OAuth2AccessToken accessToken,
        String code
    )
        throws NoSuchClientException, NoSuchResourceException, SystemException, InvalidDefinitionException, OAuth2Exception {
        Authentication userAuth = authentication.getUserAuthentication();
        if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
            throw new InvalidRequestException("id_token requires a valid user authentication");
        }

        String clientId = oauth2ClientDetails.getClientId();
        String subjectId = userDetails.getUserId();
        Set<String> scopes = request.getScope();

        ClientDetails clientDetails = clientService.loadClient(clientId);

        // build user claims
        Map<String, Object> userClaims = new HashMap<>();
        // check if client wants all claims from accessToken in idTokens
        if (
            accessToken != null && oauth2ClientDetails.isIdTokenClaims() && accessToken instanceof AACOAuth2AccessToken
        ) {
            // keep claims not overlapping reserved
            Map<String, Serializable> accessTokenClaims =
                ((AACOAuth2AccessToken) accessToken).getClaims()
                    .entrySet()
                    .stream()
                    .filter(
                        e ->
                            (!IdToken.REGISTERED_CLAIM_NAMES.contains(e.getKey()) &&
                                !IdToken.STANDARD_CLAIM_NAMES.contains(e.getKey()))
                    )
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            userClaims.putAll(accessTokenClaims);
        }

        List<Claim> claimss = new ArrayList<>();
        for (String scope : scopes) {
            // avoid adding openid claims if access token is issued
            // as per spec they should be fetched from userinfo
            // https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.4
            if (accessToken != null && OPENID_SCOPES.contains(scope)) {
                continue;
            }
            if (claimsExtractorProvider.getScopes().contains(scope)) {
                ClaimsSet cs = claimsExtractorProvider
                    .getExtractor(scope)
                    .extractUserClaims(scope, user, clientDetails, scopes, null);
                if (cs != null) {
                    claimss.addAll(cs.getClaims());
                }
            }
        }

        // flat conversion, does not support nested objects (for example address)
        for (Claim c : claimss) {
            userClaims.put(c.getKey(), c.getValue());
        }

        // add clientId as authorizedParty
        userClaims.put("azp", clientId);

        // add nonce if provided
        String nonce = (String) request.getExtensions().get(NONCE);
        if (StringUtils.hasText(nonce)) {
            userClaims.put("nonce", nonce);
        }

        // always add auth_time, not a sensitive info
        WebAuthenticationDetails authDetails = ((UserAuthentication) userAuth).getWebAuthenticationDetails();
        if (authDetails != null) {
            // timestamp in millis
            long authTimestamp = authDetails.getTimestamp();
            userClaims.put("auth_time", authTimestamp / 1000L);
        }

        if (request.getExtensions().containsKey(MAX_AGE)) {
            if (authDetails == null) {
                throw new ServerErrorException("unable to provide auth_time");
            }

            // in millis
            long curTimestamp = Instant.now().toEpochMilli();
            long authTimestamp = authDetails.getTimestamp();

            // in seconds
            long maxAge = Long.parseLong((String) request.getExtensions().get(MAX_AGE));

            if ((authTimestamp + (maxAge * 1000)) < curTimestamp) {
                // throw exception to ask for reauth (to be implemented)
                throw new InsufficientAuthenticationException("authentication too old");
            }
        }

        // build hashes for related content
        if (accessToken != null) {
            // calculate the token hash
            Base64URL at_hash = jwtService.hashAccessToken(oauth2ClientDetails, accessToken.getValue());

            // serialize to avoid json-smart bugs
            String value = at_hash != null ? at_hash.toString() : null;
            userClaims.put("at_hash", value);
        }

        if (code != null) {
            // calculate the code hash
            Base64URL c_hash = jwtService.hashCode(oauth2ClientDetails, code);

            // serialize to avoid json-smart bugs
            String value = c_hash != null ? c_hash.toString() : null;
            userClaims.put("c_hash", value);
        }

        return userClaims;
    }
}
