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

package it.smartcommunitylab.aac.openidfed.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.openidfed.OpenIdFedIdentityAuthority;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.resolvers.CachingEntityStatementResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.EntityStatementResolver;
import it.smartcommunitylab.aac.openidfed.service.DefaultOpenIdRpMetadataResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.minidev.json.JSONObject;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class OpenIdFedResolverFilter extends OncePerRequestFilter {

    public static final String OPENID_FEDERATION_URL = Config.WELL_KNOWN_URL + "/openid-federation";

    private static final String MIME_TYPE = "application/resolve-response+jwt";
    private static final JOSEObjectType JOSE_TYPE = new JOSEObjectType("resolve-response+jwt");

    public static final String DEFAULT_FILTER_URI = OpenIdFedIdentityAuthority.AUTHORITY_URL + "resolve/{providerId}";

    private final OpenIdFedErrorHttpMessageHandler errorHandler = new OpenIdFedErrorHttpMessageHandler();

    private final String authorityId;
    private final ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository;
    private RequestMatcher requestMatcher;

    public OpenIdFedResolverFilter(ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository) {
        this(SystemKeys.AUTHORITY_OPENIDFED, registrationRepository, DEFAULT_FILTER_URI);
    }

    public OpenIdFedResolverFilter(
        String authority,
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository,
        String filterProcessingUrl
    ) {
        Assert.hasText(authority, "authority can not be null or empty");
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");

        this.authorityId = authority;
        this.registrationRepository = registrationRepository;

        //build request matcher supporting only GET as per spec
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl, "GET");
    }

    @Override
    @Nullable
    protected String getFilterName() {
        return getClass().getName() + "." + authorityId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        RequestMatcher.MatchResult matcher = this.requestMatcher.matcher(request);
        if (!matcher.isMatch()) {
            chain.doFilter(request, response);
            return;
        }

        String providerId = resolveProviderId(request);
        OpenIdFedIdentityProviderConfig config = registrationRepository.findByProviderId(providerId);
        if (config == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            //extract request details
            String sub = request.getParameter("sub");
            String anchor = request.getParameter("anchor");
            String type = request.getParameter("type");

            //parameters check: anchor and sub are required
            if (!StringUtils.hasText(anchor) || !StringUtils.hasText(sub)) {
                OAuth2Error oauth2Error = new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_REQUEST,
                    "Missing required parameters [sub] or [trust_anchor]",
                    null
                );
                throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
            }

            //sanity check: anchor should match provider's
            if (!anchor.equals(config.getConfigMap().getTrustAnchor())) {
                OAuth2Error oauth2Error = new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_REQUEST,
                    "trust_anchor should match " + String.valueOf(config.getConfigMap().getTrustAnchor()),
                    null
                );
                throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
            }

            //sanity check: we know only providers
            if (StringUtils.hasText(type) && !type.equals(" federation_entity") && !type.equals("openid_provider")) {
                OAuth2Error oauth2Error = new OAuth2Error("not_found", sub + " is unknown", null);
                throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
            }

            //lookup for a matching provider already resolved
            OIDCProviderMetadata provider = config.getProviderService().findProvider(sub);
            if (provider == null) {
                OAuth2Error oauth2Error = new OAuth2Error("not_found", sub + " is unknown", null);
                throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
            }

            //retrieve resolved statement
            EntityStatementResolver entityStatementResolver = config.getEntityStatementResolver();
            EntityStatement statement = entityStatementResolver.resolveEntityStatement(anchor, sub);
            if (statement == null) {
                OAuth2Error oauth2Error = new OAuth2Error("not_found", sub + " is unknown", null);
                throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
            }

            //trust chain if available
            //note: it always matches our federation because we check for a common trust anchor
            TrustChain trustChain = null;
            if (entityStatementResolver instanceof CachingEntityStatementResolver) {
                //retrieve cached trust chain
                trustChain = ((CachingEntityStatementResolver) entityStatementResolver).resolvedTrustChain(anchor, sub);
            }

            //write response
            String metadata = writeMetadata(request, config, statement, trustChain, type);
            writeMetadataToResponse(response, metadata);
        } catch (OAuth2AuthenticationException e) {
            //unwrap and write specific error
            OAuth2Error error = e.getError();
            errorHandler.handleError(error, response);
        } catch (Exception e) {
            ///log and throw
            logger.error(e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String writeMetadata(
        HttpServletRequest request,
        OpenIdFedIdentityProviderConfig config,
        EntityStatement statement,
        TrustChain trustChain,
        String type
    ) {
        //extract relevant information
        List<TrustMarkEntry> trustMarks = Optional.ofNullable(statement.getClaimsSet().getTrustMarks()).orElse(
            Collections.emptyList()
        );
        FederationEntityMetadata federationEntityMetadata = statement.getClaimsSet().getFederationEntityMetadata();
        OIDCProviderMetadata opMetadata = statement.getClaimsSet().getOPMetadata();

        Map<String, JSONObject> metadata = new HashMap<>();
        if (!StringUtils.hasText(type)) {
            metadata.put("openid_provider", opMetadata.toJSONObject());
            metadata.put("federation_entity", federationEntityMetadata.toJSONObject());
        } else if (type.equals(" federation_entity")) {
            metadata.put("federation_entity", federationEntityMetadata.toJSONObject());
        } else if (type.equals("openid_provider")) {
            metadata.put("openid_provider", opMetadata.toJSONObject());
        }

        //expand client identifier as url
        String baseUrl = DefaultOpenIdRpMetadataResolver.extractBaseUrl(request);
        String clientId = DefaultOpenIdRpMetadataResolver.expandRedirectUri(
            baseUrl,
            config.getClientId(),
            "id"
        ).toString();

        JWK jwk = config.getFederationJWK();

        try {
            JWSAlgorithm jwsAlgorithm = resolveAlgorithm(jwk);
            if (jwsAlgorithm == null) {
                return null;
            }

            JWSSigner signer = buildJwsSigner(jwk, jwsAlgorithm);
            if (signer == null) {
                return null;
            }

            Date issuedAt = Date.from(Instant.now());
            Date expiresAt = statement.getClaimsSet().getExpirationTime();

            JWSHeader header = new JWSHeader.Builder(jwsAlgorithm).keyID(jwk.getKeyID()).type(JOSE_TYPE).build();

            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(statement.getClaimsSet().getSubject().getValue())
                .issueTime(issuedAt)
                .expirationTime(expiresAt);

            //add response metadata
            claims.claim("metadata", metadata);

            //add trust marks
            if (trustMarks != null) {
                claims.claim(
                    "trust_marks",
                    trustMarks.stream().map(t -> t.toJSONObject()).collect(Collectors.toList())
                );
            }

            //add trust chain
            if (trustChain != null) {
                claims.claim("trust_chain", trustChain.toSerializedJWTs());
            }

            //sign and return as string
            SignedJWT jwt = new SignedJWT(header, claims.build());
            jwt.sign(signer);

            return jwt.serialize();
        } catch (JOSEException e) {
            throw new JwtEncodingException("error encoding the jwt with the provided key");
        }
    }

    private void writeMetadataToResponse(HttpServletResponse response, String metadata) throws IOException {
        response.setContentType(MIME_TYPE);
        response.setContentLength(metadata.length());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(metadata);
    }

    private String resolveProviderId(HttpServletRequest request) {
        RequestMatcher.MatchResult matcher = this.requestMatcher.matcher(request);
        if (matcher.isMatch()) {
            return matcher.getVariables().get("providerId");
        }

        return null;
    }

    private JWSSigner buildJwsSigner(JWK jwk, JWSAlgorithm jwsAlgorithm) throws JOSEException {
        if (KeyType.RSA.equals(jwk.getKeyType())) {
            return new RSASSASigner(jwk.toRSAKey());
        } else if (KeyType.EC.equals(jwk.getKeyType())) {
            return new ECDSASigner(jwk.toECKey());
        } else if (KeyType.OCT.equals(jwk.getKeyType())) {
            return new MACSigner(jwk.toOctetSequenceKey());
        }

        return null;
    }

    private JWSAlgorithm resolveAlgorithm(JWK jwk) {
        String jwsAlgorithm = null;
        if (jwk.getAlgorithm() != null) {
            jwsAlgorithm = jwk.getAlgorithm().getName();
        }

        if (jwsAlgorithm == null) {
            if (KeyType.RSA.equals(jwk.getKeyType())) {
                jwsAlgorithm = SignatureAlgorithm.RS256.getName();
            } else if (KeyType.EC.equals(jwk.getKeyType())) {
                jwsAlgorithm = SignatureAlgorithm.ES256.getName();
            } else if (KeyType.OCT.equals(jwk.getKeyType())) {
                jwsAlgorithm = MacAlgorithm.HS256.getName();
            }
        }

        return jwsAlgorithm == null ? null : JWSAlgorithm.parse(jwsAlgorithm);
    }
}
