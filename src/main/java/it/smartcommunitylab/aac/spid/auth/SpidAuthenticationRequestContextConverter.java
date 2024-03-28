/*
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

package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml.saml2.core.impl.AuthnRequestMarshaller;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml.saml2.core.impl.NameIDPolicyBuilder;
import org.opensaml.saml.saml2.core.impl.RequestedAuthnContextBuilder;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class SpidAuthenticationRequestContextConverter
    implements Converter<Saml2AuthenticationRequestContext, AuthnRequest> {
    static {
        OpenSamlInitializationService.initialize();
    }

    private final ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository;

    private AuthnRequestMarshaller marshaller;

    private AuthnRequestBuilder authnRequestBuilder;

    private IssuerBuilder issuerBuilder;

    private NameIDPolicyBuilder nameIDPolicyBuilder;

    private RequestedAuthnContextBuilder reqAuthnContextBuilder;

    public SpidAuthenticationRequestContextConverter(
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository
    ) {
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        this.registrationRepository = registrationRepository;

        // fetch opensaml builders
        XMLObjectProviderRegistry registry = ConfigurationService.get(XMLObjectProviderRegistry.class);
        this.marshaller =
            (AuthnRequestMarshaller) registry.getMarshallerFactory().getMarshaller(AuthnRequest.DEFAULT_ELEMENT_NAME);
        this.authnRequestBuilder =
            (AuthnRequestBuilder) registry.getBuilderFactory().getBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME);
        this.issuerBuilder = (IssuerBuilder) registry.getBuilderFactory().getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
        this.nameIDPolicyBuilder =
            (NameIDPolicyBuilder) registry.getBuilderFactory().getBuilder(NameIDPolicy.DEFAULT_ELEMENT_NAME);
        this.reqAuthnContextBuilder =
            (RequestedAuthnContextBuilder) registry
                .getBuilderFactory()
                .getBuilder(RequestedAuthnContext.DEFAULT_ELEMENT_NAME);
    }

    @Override
    public AuthnRequest convert(Saml2AuthenticationRequestContext context) {
        // fetch registration id and complete configuration
        RelyingPartyRegistration relyingPartyRegistration = context.getRelyingPartyRegistration();
        String registrationId = relyingPartyRegistration.getRegistrationId();

        // registrationId is providerId+idpkey
        String providerId = SpidIdentityProviderConfig.getProviderId(registrationId);
        SpidIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(providerId);
        if (providerConfig == null) {
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                "No relying party registration found"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }

        // extract baseUrl
        Map<String, String> acsVariables = new HashMap<>();
        acsVariables.put("baseUrl", "");
        acsVariables.put("registrationId", registrationId);
        UriComponents acsComponents = UriComponentsBuilder
            .fromUriString(SpidIdentityProviderConfig.DEFAULT_CONSUMER_URL)
            .replaceQuery(null)
            .fragment(null)
            .buildAndExpand(acsVariables);
        UriComponents uriComponents = UriComponentsBuilder
            .fromHttpUrl(relyingPartyRegistration.getAssertionConsumerServiceLocation())
            .replacePath(acsComponents.getPath())
            .replaceQuery(null)
            .fragment(null)
            .build();
        String baseUrl = uriComponents.toUriString();
        String realmUrl = baseUrl + "/-/" + providerConfig.getRealm();

        // build request base
        String issuer = context.getIssuer();
        String destination = context.getDestination();
        String protocolBinding = context.getRelyingPartyRegistration().getAssertionConsumerServiceBinding().getUrn();

        AuthnRequest auth = authnRequestBuilder.buildObject();
        auth.setID("ARQ" + UUID.randomUUID().toString().substring(1));
        auth.setIssueInstant(Instant.now());
        auth.setProtocolBinding(protocolBinding);

        Issuer iss = this.issuerBuilder.buildObject();
        iss.setValue(issuer);
        iss.setFormat(NameIDType.ENTITY);
        iss.setNameQualifier(realmUrl);

        auth.setIssuer(iss);
        auth.setDestination(destination);
        auth.setAssertionConsumerServiceURL(context.getAssertionConsumerServiceUrl());

        NameIDPolicy nameIDPolicy = nameIDPolicyBuilder.buildObject();
        nameIDPolicy.setFormat(NameIDType.TRANSIENT);
        auth.setNameIDPolicy(nameIDPolicy);

        if (providerConfig.getRelyingPartyRegistrationIsForceAuthn() != null) {
            auth.setForceAuthn(providerConfig.getRelyingPartyRegistrationIsForceAuthn());
        }

        if (providerConfig.getRelyingPartyRegistrationAuthnContextClassRefs() != null) {
            RequestedAuthnContext requestAuthnContext = this.reqAuthnContextBuilder.buildObject();
            requestAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.MINIMUM);
            providerConfig
                .getRelyingPartyRegistrationAuthnContextClassRefs()
                .forEach(r -> {
                    AuthnContextClassRef authnContextClassRef = new AuthnContextClassRefBuilder().buildObject();
                    //                authnContextClassRef.setAuthnContextClassRef(r);
                    authnContextClassRef.setURI(r); // TODO: evaluate this option as a possible update from the previous deprecated method usage
                    requestAuthnContext.getAuthnContextClassRefs().add(authnContextClassRef);
                });

            auth.setRequestedAuthnContext(requestAuthnContext);
        }

        // assertion consuming
        // TODO: review with ACR
        auth.setAssertionConsumerServiceIndex(0);
        auth.setAttributeConsumingServiceIndex(0);

        //        Scoping scoping = new ScopingBuilder().buildObject();
        //        scoping.setProxyCount(0);
        //        auth.setScoping(scoping);
        return auth;
        //        return null;
    }
}
