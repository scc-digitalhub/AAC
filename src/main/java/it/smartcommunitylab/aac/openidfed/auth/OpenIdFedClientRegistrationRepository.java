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

import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openidfed.service.OpenIdProviderDiscoveryService;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OpenIdFedClientRegistrationRepository implements ClientRegistrationRepository {

    //config
    private final OpenIdFedIdentityProviderConfigMap configMap;
    private final String redirectUriTemplate;

    // resolver
    private final OpenIdProviderDiscoveryService discoveryService;

    //use base64 encoders for entityIds by default
    private UnaryOperator<String> decoder = value -> {
        return new String(Base64.getUrlDecoder().decode(value.getBytes()));
    };
    private UnaryOperator<String> encoder = value -> {
        return Base64.getUrlEncoder().encodeToString(value.getBytes());
    };

    //TODO loading cache, must evaluate exp on federation metadata

    public OpenIdFedClientRegistrationRepository(
        OpenIdFedIdentityProviderConfigMap configMap,
        OpenIdProviderDiscoveryService discoveryService,
        String redirectUriTemplate
    ) {
        Assert.notNull(configMap, "config map  can not be null");
        Assert.notNull(discoveryService, "discovery service  can not be null");
        Assert.hasText(redirectUriTemplate, "redirectUriTemplate is required");

        this.configMap = configMap;
        this.redirectUriTemplate = redirectUriTemplate;
        this.discoveryService = discoveryService;
    }

    public void setDecoder(UnaryOperator<String> decoder) {
        this.decoder = decoder;
    }

    public void setEncoder(UnaryOperator<String> encoder) {
        this.encoder = encoder;
    }

    public String encode(String entityId) {
        return this.encoder.apply(entityId);
    }

    public String decode(String registrationId) {
        return this.decoder.apply(registrationId);
    }

    /*
     * generate client registrations for every OP identified by registrationId
     */

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registration id cannot be empty");
        //extract entityId from registrationId: base64/url encoded
        String entityId = decoder.apply(registrationId);
        //fetch metadata for the given entityId

        OIDCProviderMetadata provider = discoveryService.findProvider(entityId);
        if (provider == null) {
            return null;
        }

        return toClientRegistration(provider);
    }

    private ClientRegistration toClientRegistration(OIDCProviderMetadata metadata) {
        if (metadata.getIssuer() == null || !StringUtils.hasText(metadata.getIssuer().getValue())) {
            // unsupported without issuer
            return null;
        }

        //autoconf via builder,
        ClientRegistration.Builder builder = ClientRegistrations.fromIssuerLocation(metadata.getIssuer().getValue());

        //we support only automatic registration with private keys for now
        ClientAuthenticationMethod clientAuthenticationMethod = ClientAuthenticationMethod.PRIVATE_KEY_JWT;
        builder.clientAuthenticationMethod(clientAuthenticationMethod);

        String[] scope = StringUtils.commaDelimitedListToStringArray(configMap.getScope());
        //always add "openid"
        Set<String> scopes = new HashSet<>(Arrays.asList(scope));
        scopes.add("openid");
        builder.scope(scopes);

        //use sub as default userName
        String userNameAttributeName = StringUtils.hasText(configMap.getUserNameAttributeName())
            ? configMap.getUserNameAttributeName()
            : StandardClaimNames.SUB;
        builder.userNameAttributeName(userNameAttributeName);

        // we support only authCode
        builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
        // add our redirect template
        builder.redirectUri(redirectUriTemplate);

        // set a placeholder for client id
        builder.clientId("_CLIENT_ID_");
        builder.clientName(configMap.getClientName());

        //set registrationId
        String registrationId = encoder.apply(metadata.getIssuer().getValue());
        builder.registrationId(registrationId);

        return builder.build();
    }
}
