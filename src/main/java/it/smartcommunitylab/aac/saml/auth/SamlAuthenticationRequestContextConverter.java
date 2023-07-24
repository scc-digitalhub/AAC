package it.smartcommunitylab.aac.saml.auth;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import java.time.Instant;
import java.util.UUID;
import org.joda.time.DateTime;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
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
import org.springframework.util.Assert;

public class SamlAuthenticationRequestContextConverter
    implements Converter<Saml2AuthenticationRequestContext, AuthnRequest> {
    static {
        OpenSamlInitializationService.initialize();
    }

    private final ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository;

    private AuthnRequestMarshaller marshaller;

    private AuthnRequestBuilder authnRequestBuilder;

    private IssuerBuilder issuerBuilder;

    private NameIDPolicyBuilder nameIDPolicyBuilder;

    private RequestedAuthnContextBuilder reqAuthnContextBuilder;

    public SamlAuthenticationRequestContextConverter(
        ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository
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
        String registrationId = context.getRelyingPartyRegistration().getRegistrationId();

        // registrationId is providerId
        SamlIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(registrationId);
        if (providerConfig == null) {
            Saml2Error saml2Error = new Saml2Error(
                Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                "No relying party registration found"
            );
            throw new Saml2AuthenticationException(saml2Error);
        }

        // build request base
        Instant now = Instant.now();
        String issuer = context.getIssuer();
        String destination = context.getDestination();
        String protocolBinding = context.getRelyingPartyRegistration().getAssertionConsumerServiceBinding().getUrn();

        AuthnRequest auth = authnRequestBuilder.buildObject();
        auth.setID("ARQ" + UUID.randomUUID().toString().substring(1));
        auth.setIssueInstant(new DateTime(now.getEpochSecond() * 1000));
        auth.setForceAuthn(Boolean.FALSE);
        auth.setIsPassive(Boolean.FALSE);
        auth.setProtocolBinding(protocolBinding);

        Issuer iss = this.issuerBuilder.buildObject();
        iss.setValue(issuer);
        auth.setIssuer(iss);
        auth.setDestination(destination);
        auth.setAssertionConsumerServiceURL(context.getAssertionConsumerServiceUrl());

        // check additional config
        if (providerConfig.getRelyingPartyRegistrationIsForceAuthn() != null) {
            auth.setForceAuthn(providerConfig.getRelyingPartyRegistrationIsForceAuthn());
        }
        if (providerConfig.getRelyingPartyRegistrationIsPassive() != null) {
            auth.setIsPassive(providerConfig.getRelyingPartyRegistrationIsPassive());
        }

        if (providerConfig.getRelyingPartyRegistrationNameIdFormat() != null) {
            NameIDPolicy nameIDPolicy = nameIDPolicyBuilder.buildObject();
            nameIDPolicy.setFormat(providerConfig.getRelyingPartyRegistrationNameIdFormat());
            nameIDPolicy.setAllowCreate(false);
            if (providerConfig.getRelyingPartyRegistrationNameIdAllowCreate() != null) {
                nameIDPolicy.setAllowCreate(providerConfig.getRelyingPartyRegistrationNameIdAllowCreate());
            }

            auth.setNameIDPolicy(nameIDPolicy);
        }

        if (providerConfig.getRelyingPartyRegistrationAuthnContextClassRefs() != null) {
            RequestedAuthnContext requestAuthnContext = this.reqAuthnContextBuilder.buildObject();
            requestAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.EXACT);
            providerConfig
                .getRelyingPartyRegistrationAuthnContextClassRefs()
                .forEach(r -> {
                    AuthnContextClassRef authnContextClassRef = new AuthnContextClassRefBuilder().buildObject();
                    authnContextClassRef.setAuthnContextClassRef(r);
                    requestAuthnContext.getAuthnContextClassRefs().add(authnContextClassRef);
                });

            if (providerConfig.getRelyingPartyRegistrationAuthnContextComparison() != null) {
                String comparison = providerConfig.getRelyingPartyRegistrationAuthnContextComparison();
                if ("minimum".equals(comparison)) {
                    requestAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.MINIMUM);
                } else if ("maximum".equals(comparison)) {
                    requestAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.MAXIMUM);
                } else if ("better".equals(comparison)) {
                    requestAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.BETTER);
                }
            }
            auth.setRequestedAuthnContext(requestAuthnContext);
        }

        //        Scoping scoping = new ScopingBuilder().buildObject();
        //        scoping.setProxyCount(2);
        //        auth.setScoping(scoping);

        return auth;
    }
}
