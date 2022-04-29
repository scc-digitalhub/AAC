package it.smartcommunitylab.aac.openid.apple.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.endpoint.AbstractOAuth2AuthorizationGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfigMap;

public class AppleClientAuthenticationParametersConverter<T extends AbstractOAuth2AuthorizationGrantRequest>
        implements Converter<T, MultiValueMap<String, String>> {

    private final AppleIdentityProviderConfigMap configMap;
    private JwtEncoder jwtEncoder;

    public AppleClientAuthenticationParametersConverter(AppleIdentityProviderConfig config) {
        Assert.notNull(config, "config must be set");
        this.configMap = config.getConfigMap();

        JWK jwk = config.getClientJWK();

        Assert.notNull(jwk, "jwk must be set");
        Assert.isTrue(jwk.getKeyType().equals(KeyType.EC), "key must be of type ES256");

        // build encoder with this key
        jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    @Override
    public MultiValueMap<String, String> convert(T authorizationGrantRequest) {
        Assert.notNull(authorizationGrantRequest, "authorizationGrantRequest cannot be null");

        ClientRegistration clientRegistration = authorizationGrantRequest.getClientRegistration();

        // build jwt with assertions as per
        // https://developer.apple.com/documentation/sign_in_with_apple/generate_and_validate_tokens

        JwsHeader.Builder headersBuilder = JwsHeader
                .with(SignatureAlgorithm.ES256)
                .keyId(configMap.getKeyId());

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofSeconds(120));

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(configMap.getTeamId())
                .subject(clientRegistration.getClientId())
                .audience(Collections.singletonList(AppleIdentityProviderConfig.ISSUER_URI))
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt);

        // build and sign
        JwsHeader jwsHeader = headersBuilder.build();
        JwtClaimsSet jwtClaimsSet = claimsBuilder.build();
        Jwt jws = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, jwtClaimsSet));

        // set as secret
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.set(OAuth2ParameterNames.CLIENT_SECRET, jws.getTokenValue());

        return parameters;
    }

}
