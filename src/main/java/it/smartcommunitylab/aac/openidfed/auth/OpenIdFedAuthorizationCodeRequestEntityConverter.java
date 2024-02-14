/**
 * Copyright 2024 the original author or authors
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

import com.nimbusds.jose.jwk.JWK;
import it.smartcommunitylab.aac.oidc.events.OAuth2TokenRequestEvent;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import java.util.function.Function;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.util.Assert;

public class OpenIdFedAuthorizationCodeRequestEntityConverter
    extends OAuth2AuthorizationCodeGrantRequestEntityConverter
    implements ApplicationEventPublisherAware {

    private final OpenIdFedIdentityProviderConfig config;

    private ApplicationEventPublisher eventPublisher;

    public OpenIdFedAuthorizationCodeRequestEntityConverter(OpenIdFedIdentityProviderConfig config) {
        Assert.notNull(config, "provider config is required");
        this.config = config;

        // private key jwt resolver, as per
        // https://tools.ietf.org/html/rfc7523#section-2.2
        // fetch key
        JWK jwk = config.getClientSignatureJWK();

        // build resolver only for this registration to retrieve client key
        Function<ClientRegistration, JWK> jwkResolver = clientRegistration -> jwk;
        addParametersConverter(new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver));
    }

    @Override
    public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest authorizationGrantRequest) {
        RequestEntity<?> request = super.convert(authorizationGrantRequest);
        if (eventPublisher != null) {
            OAuth2TokenRequestEvent event = new OAuth2TokenRequestEvent(
                config.getAuthority(),
                config.getProvider(),
                config.getRealm(),
                request
            );
            event.setTx(authorizationGrantRequest.getAuthorizationExchange().getAuthorizationRequest().getState());

            eventPublisher.publishEvent(event);
        }

        return request;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
}
