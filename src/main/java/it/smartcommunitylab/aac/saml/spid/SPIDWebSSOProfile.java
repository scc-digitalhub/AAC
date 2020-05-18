package it.smartcommunitylab.aac.saml.spid;

import org.opensaml.common.SAMLException;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.websso.WebSSOProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfileOptions;

public class SPIDWebSSOProfile extends WebSSOProfileImpl {

    @Override
    protected AuthnRequest getAuthnRequest(SAMLMessageContext context, WebSSOProfileOptions options,
            AssertionConsumerService assertionConsumer,
            SingleSignOnService bindingService) throws SAMLException, MetadataProviderException {
        AuthnRequest request = super.getAuthnRequest(context, options, assertionConsumer, bindingService);

        // we need to explicitly set request issuer
        buildIssuer(request, options);

        // we also need to request attributes as assertions
        setAssertionIndex(request);

        return request;
    }

    private void setAssertionIndex(AuthnRequest request) {
        // set according to SP metadata: as per SPID we need to ASK for attributes
        request.setAttributeConsumingServiceIndex(1);

    }

    private void buildIssuer(AuthnRequest request, WebSSOProfileOptions options) {

        // we extract the issuer name from the pre-build request
        // TODO make configurable
        String issuerId = request.getIssuer().getValue();
        IssuerBuilder issuerBuilder = new IssuerBuilder();
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setNameQualifier(issuerId);
        issuer.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:entity");
        issuer.setValue(issuerId);
        request.setIssuer(issuer);

    }
}
