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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.Claim;
import it.smartcommunitylab.aac.claims.ClaimsSet;
import it.smartcommunitylab.aac.clients.service.ClientDetailsService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.jwt.JWTService;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.profiles.claims.OpenIdClaimsExtractorProvider;
import it.smartcommunitylab.aac.users.auth.UserAuthentication;
import it.smartcommunitylab.aac.users.model.User;
import it.smartcommunitylab.aac.users.model.UserDetails;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Build an ID token via profile
 */

public class OIDCTokenEnhancer implements TokenEnhancer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Set<String> REGISTERED_CLAIM_NAMES = JWTClaimsSet.getRegisteredNames();
    private static final Set<String> STANDARD_CLAIM_NAMES = IDTokenClaimsSet.getStandardClaimNames();

    public static final String MAX_AGE = "max_age";
    public static final String NONCE = "nonce";
    public static final String AUTH_TIMESTAMP = "AUTH_TIMESTAMP";

    private final String issuer;

    // TODO evaluate drop claimService, idToken could be standard only
    // at minimum we should split claim mapping for access and id tokens
    //    private final ClaimsService claimsService;

    // provide only standard claims
    private final OpenIdClaimsExtractorProvider claimsExtractorProvider;

    private final JWTService jwtService;

    private final ClientDetailsService clientDetailsService;
    private final OAuth2ClientDetailsService oauth2ClientDetailsService;

    public OIDCTokenEnhancer(
        String issuer,
        JWTService jwtService,
        ClientDetailsService clientDetailsService,
        OAuth2ClientDetailsService oauth2ClientDetailsService,
        OpenIdClaimsExtractorProvider openidClaimsExtractorProvider
    ) {
        //            ClaimsService claimsService) {
        Assert.hasText(issuer, "a valid issuer is required");
        Assert.notNull(jwtService, "jwt service is mandatory to sign tokens");
        Assert.notNull(clientDetailsService, "client details service is mandatory");
        Assert.notNull(oauth2ClientDetailsService, "oauth2 client details service is mandatory");
        //        Assert.notNull(claimsService, "claims service is mandatory");
        Assert.notNull(openidClaimsExtractorProvider, "openid claims extractor provider is mandatory");

        this.issuer = issuer;
        this.jwtService = jwtService;
        this.clientDetailsService = clientDetailsService;
        this.oauth2ClientDetailsService = oauth2ClientDetailsService;
        //        this.claimsService = claimsService;
        this.claimsExtractorProvider = openidClaimsExtractorProvider;
    }

    @Override
    public AACOAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        logger.debug(
            "enhance access token " +
            accessToken.getTokenType() +
            " for " +
            authentication.getName() +
            " value " +
            accessToken.toString()
        );

        OAuth2Request request = authentication.getOAuth2Request();
        String clientId = request.getClientId();
        Set<String> scopes = request.getScope();

        AACOAuth2AccessToken token = new AACOAuth2AccessToken(accessToken);

        // evaluate if request is for an idToken
        //        if (!scopes.contains(Config.SCOPE_OPENID) || !request.getResponseTypes().contains("id_token")) {
        if (!scopes.contains(Config.SCOPE_OPENID)) {
            // nothing to do
            return token;
        }

        try {
            ClientDetails clientDetails = clientDetailsService.loadClient(clientId);
            OAuth2ClientDetails oauth2ClientDetails = oauth2ClientDetailsService.loadClientByClientId(clientId);

            logger.debug("fetch user via authentication");

            Authentication userAuth = authentication.getUserAuthentication();
            if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
                throw new InvalidRequestException("id_token requires a valid user authentication");
            }

            UserDetails userDetails = ((UserAuthentication) userAuth).getUserDetails();
            User user = ((UserAuthentication) userAuth).getUser();

            JWT idToken = createIdToken(request, accessToken, userDetails, clientDetails, oauth2ClientDetails, user);

            token.setIdToken(idToken);

            return token;
        } catch (NoSuchClientException | ClientRegistrationException e) {
            logger.error("non existing client: " + e.getMessage());
            throw new InvalidClientException("invalid client");
        } catch (SystemException | NoSuchResourceException | InvalidDefinitionException e) {
            logger.error("claims service error: " + e.getMessage());
            throw new OAuth2Exception(e.getMessage());
        }
    }

    private JWT createIdToken(
        OAuth2Request request,
        OAuth2AccessToken accessToken,
        UserDetails userDetails,
        ClientDetails clientDetails,
        OAuth2ClientDetails oauth2ClientDetails,
        User user
    ) throws NoSuchResourceException, InvalidDefinitionException, SystemException {
        logger.trace("access token used for oidc is " + accessToken);

        String clientId = clientDetails.getClientId();
        String subjectId = userDetails.getUserId();
        Set<String> scopes = request.getScope();
        Set<String> resourceIds = request.getResourceIds();

        // build claims set according to OIDC 1.0
        JWTClaimsSet.Builder idClaims = new JWTClaimsSet.Builder();

        //        // ask claim Manager for user claims
        //        // TODO evaluate splitting claims from accessToken or dropping extended claims
        //        // on idToken. Besides we already have claims here, why do again if we get the
        //        // same result
        //        Map<String, Serializable> userClaims = claimsService.getUserClaims(
        //                userDetails, clientDetails.getRealm(),
        //                clientDetails, scopes,
        //                resourceIds);

        Map<String, Serializable> userClaims = new HashMap<>();

        // check if client wants all claims from accessToken in idTokens
        if (oauth2ClientDetails.isIdTokenClaims() && accessToken instanceof AACOAuth2AccessToken) {
            // keep claims not overlapping reserved
            Map<String, Serializable> accessTokenClaims =
                ((AACOAuth2AccessToken) accessToken).getClaims()
                    .entrySet()
                    .stream()
                    .filter(
                        e ->
                            (!REGISTERED_CLAIM_NAMES.contains(e.getKey()) && !STANDARD_CLAIM_NAMES.contains(e.getKey()))
                    )
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            userClaims.putAll(accessTokenClaims);
        }

        List<Claim> claimss = new ArrayList<>();
        for (String scope : scopes) {
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

        // set via builder
        userClaims.entrySet().forEach(e -> idClaims.claim(e.getKey(), e.getValue()));

        // if the auth time claim was explicitly requested OR if the client always wants
        // the auth time, put it in
        // TODO check "idtoken" vs "id_token"
        if (request.getExtensions().containsKey(MAX_AGE) || (request.getExtensions().containsKey("idtoken"))) {
            if (request.getExtensions().get(AUTH_TIMESTAMP) != null) {
                Long authTimestamp = Long.parseLong((String) request.getExtensions().get(AUTH_TIMESTAMP));
                if (authTimestamp != null) {
                    idClaims.claim("auth_time", authTimestamp / 1000L);
                }
            } else {
                // we couldn't find the timestamp!
                logger.warn(
                    "Unable to find authentication timestamp! There is likely something wrong with the configuration."
                );
            }
        }

        idClaims.issueTime(new Date());

        if (accessToken.getExpiration() != null) {
            Date expiration = accessToken.getExpiration();
            idClaims.expirationTime(expiration);
        }

        // DISABLED multiple audiences same as accessToken
        //        List<String> audiences = new LinkedList<>();
        //        audiences.add(clientId);
        //        audiences.addAll(getServiceIds(request.getScope()));
        //      idClaims.audience(audiences);

        // set single audience as string - correct for OIDC
        idClaims.audience(clientId);

        idClaims.issuer(issuer);
        idClaims.subject(subjectId);
        idClaims.jwtID(UUID.randomUUID().toString());
        idClaims.claim("azp", clientId);

        String nonce = (String) request.getExtensions().get(NONCE);
        if (StringUtils.hasText(nonce)) {
            idClaims.claim("nonce", nonce);
        }

        // add additional claims for scopes
        // DISABLED, not in the spec
        //		idClaims.claim("scope", String.join(" ", request.getScope()));

        Set<String> responseTypes = request.getResponseTypes();

        // at_hash is used for both implicit and auth_code flows when paired with
        // accessToken
        if (responseTypes.contains("token") || responseTypes.contains("code")) {
            // calculate the token hash
            Base64URL at_hash = jwtService.hashAccessToken(oauth2ClientDetails, accessToken.getValue());
            idClaims.claim("at_hash", at_hash);
        }

        JWTClaimsSet claims = idClaims.build();
        logger.trace("idToken claims " + claims.toString());

        JWT idToken = jwtService.buildAndSignJWT(oauth2ClientDetails, claims);

        logger.trace("idToken result " + idToken.serialize());

        return idToken;
    }
}
