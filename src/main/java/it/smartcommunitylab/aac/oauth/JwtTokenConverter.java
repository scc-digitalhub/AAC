package it.smartcommunitylab.aac.oauth;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;

import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.jwt.JWTService;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.model.TokenType;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;

public class JwtTokenConverter implements TokenEnhancer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String issuer;
    private final JWTService jwtService;

    private final OAuth2ClientDetailsService oauth2ClientDetailsService;

    private boolean useJwtByDefault = true;

    public JwtTokenConverter(String issuer, JWTService jwtService,
            OAuth2ClientDetailsService oauth2ClientDetailsService) {
        Assert.hasText(issuer, "a valid issuer is required");
        Assert.notNull(jwtService, "jwt service is mandatory to sign tokens");
        Assert.notNull(oauth2ClientDetailsService, "oauth2 client details service is mandatory");

        this.issuer = issuer;
        this.jwtService = jwtService;
        this.oauth2ClientDetailsService = oauth2ClientDetailsService;
    }

    public void setUseJwtByDefault(boolean useJwtByDefault) {
        this.useJwtByDefault = useJwtByDefault;
    }

    @Override
    public AACOAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        logger.debug("enhance access token " + accessToken.getTokenType() + " for " + authentication.getName()
                + " value " + accessToken.toString());

        OAuth2Request request = authentication.getOAuth2Request();
        String clientId = request.getClientId();

        try {
            AACOAuth2AccessToken token = new AACOAuth2AccessToken(accessToken);

            OAuth2ClientDetails clientDetails = oauth2ClientDetailsService.loadClientByClientId(clientId);

            // client requested JWT?
            TokenType tokenType = TokenType.parse(clientDetails.getTokenType());
            boolean requireJwt = (tokenType != null ? TokenType.JWT == tokenType : useJwtByDefault);

            if (!requireJwt) {
                // nothing to do, return opaque
                return token;
            }

            UserDetails userDetails = null;
            logger.debug("fetch user via authentication");
            Authentication userAuth = authentication.getUserAuthentication();
            if (userAuth != null && (userAuth instanceof UserAuthentication)) {
                userDetails = ((UserAuthentication) userAuth).getUser();
            }

            JWT jwt = buildJWT(request, token, userDetails, clientDetails);

            if (jwt == null) {
                throw new OAuth2Exception("error with jwt");
            }

            // serialize to string
            String result = jwt.serialize();
            logger.debug("signed jwt token " + result);

            // replace value
            token.setValue(result);
            token.setResponseType(TokenType.JWT.getValue());

            return token;

        } catch (ClientRegistrationException e) {
            logger.error("non existing client: " + e.getMessage());
            throw new InvalidClientException("invalid client");
        } catch (SystemException e) {
            logger.error("jwt service error: " + e.getMessage());
            throw new OAuth2Exception(e.getMessage());
        }
    }

    private JWT buildJWT(OAuth2Request request, AACOAuth2AccessToken accessToken, UserDetails userDetails,
            OAuth2ClientDetails clientDetails) {

        logger.trace("access token used for oidc is " + accessToken);

        String clientId = clientDetails.getClientId();
        String subjectId = clientId;
        if (userDetails != null) {
            // this is a user token, use subjectId
            subjectId = userDetails.getSubjectId();
        }

        Set<String> scopes = request.getScope();
        Set<String> resourceIds = request.getResourceIds();

        // we use token raw value as JTI
        // TODO evaluate dedicated field, are we sure this is globally unique?
        String tokenId = accessToken.getToken();

        // build claims set
        JWTClaimsSet.Builder jwtClaims = new JWTClaimsSet.Builder();

        // first add all claims from map, later we will overwrite system claims
        Map<String, Serializable> claims = accessToken.getClaims();
        // add all claims, avoiding registered
        claims.entrySet().forEach(e -> {
            if (!JWTClaimsSet.getRegisteredNames().contains(e.getKey())) {
                jwtClaims.claim(e.getKey(), e.getValue());
            }
        });

        // system claims
        jwtClaims.jwtID(tokenId);
        jwtClaims.issuer(issuer);
        jwtClaims.subject(subjectId);
        // realm is provided only if produced by claimService

        // audience: we map clientId + resourceIds if present
        Set<String> audiences = new HashSet<>();
        audiences.add(clientId);
        if (resourceIds != null) {
            audiences.addAll(resourceIds);
        }

        jwtClaims.audience(new ArrayList<>(audiences));
        if (audiences.size() > 1) {
            // set client as azp only if more than one aud
            jwtClaims.claim("azp", clientId);
        }

        // time
        if (accessToken.getExpiration() != null) {
            jwtClaims.issueTime(accessToken.getIssuedAt());
            jwtClaims.notBeforeTime(accessToken.getNotBeforeTime());
            jwtClaims.expirationTime(accessToken.getExpiration());
        }

        // we add scopes even if it's outside spec
        if (scopes.isEmpty()) {
            jwtClaims.claim("scope", "");
        } else {
            jwtClaims.claim("scope", String.join(" ", scopes));
        }
        // build
        JWTClaimsSet jwtClaimsSet = jwtClaims.build();
        logger.trace("dump jwtClaims " + jwtClaimsSet.toString());

        JWT jwt = jwtService.buildAndSignJWT(clientDetails, jwtClaimsSet);

        return jwt;
    }
}
