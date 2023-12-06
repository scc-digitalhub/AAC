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

import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.resolvers.OpenIdProviderResolver;
import java.util.function.UnaryOperator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OpenIdFedClientRegistrationRepository implements ClientRegistrationRepository {

    private static final String WELL_KNOWN_CONFIGURATION_OPENID = "/.well-known/openid-configuration";
    private static final String WELL_KNOWN_FEDERATION_OPENID = "/.well-known/openid-federation";

    // resolver
    private final OpenIdProviderResolver resolver;
    private UnaryOperator<String> decoder = value -> (value);

    //TODO loading cache

    public OpenIdFedClientRegistrationRepository(OpenIdFedIdentityProviderConfig config) {
        Assert.notNull(config, "provider configuration can not be null");
        this.config = config;
    }

    public void setDecoder(UnaryOperator<String> decoder) {
        this.decoder = decoder;
    }

    /*
     * generate client registrations for every OP identified by registrationId
     */

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registration id cannot be empty");
        //extract entityId from registrationId: base64 encoded
        String entityId = decoder.apply(registrationId);
        //fetch metadata for the given entityId
        return config.getClientRegistration(entityId);
    }

    private ClientRegistration toClientRegistration(String issuerUri) {
        if (!StringUtils.hasText(issuerUri)) {
            // unsupported
            return null;
        }

        //autoconf
        // remove well-known path if provided by user
        if (issuerUri.endsWith(WELL_KNOWN_CONFIGURATION_OPENID)) {
            issuerUri = issuerUri.substring(0, issuerUri.length() - WELL_KNOWN_CONFIGURATION_OPENID.length());
        }

        // via builder,
        // providerId is unique, use as registrationId
        ClientRegistration.Builder builder = ClientRegistrations.fromIssuerLocation(issuerUri);

        //we support only automatic registration with private keys for now
        ClientAuthenticationMethod clientAuthenticationMethod = ClientAuthenticationMethod.PRIVATE_KEY_JWT;
        builder.clientAuthenticationMethod(clientAuthenticationMethod);

        String[] scope = StringUtils.commaDelimitedListToStringArray(configMap.getScope());
        builder.scope(scope);

        builder.userNameAttributeName(configMap.getUserNameAttributeName());

        // we support only authCode
        builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
        // add our redirect template
        builder.redirectUri(getRedirectUrl());

        // set client
        builder.clientId(configMap.getClientId());
        builder.clientName(configMap.getClientName());

        // re-set registrationId since auto-configuration sets values provided from
        // issuer
        builder.registrationId(getProvider());

        return builder.build();
    }
}
