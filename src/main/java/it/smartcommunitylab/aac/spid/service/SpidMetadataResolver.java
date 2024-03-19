package it.smartcommunitylab.aac.spid.service;

import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.metadata.Saml2MetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;

// currently, the SPID metadata resolver simply wraps the opensaml resolver
public class SpidMetadataResolver implements Saml2MetadataResolver  {

    public static final String SPID_XLM_NS_URI = "https://spid.gov.it/saml-extensions";

    static {
        OpenSamlInitializationService.initialize();
    }

    @Override
    public String resolve(RelyingPartyRegistration relyingPartyRegistration) {
//        String registrationId = relyingPartyRegistration.getRegistrationId();
        OpenSamlMetadataResolver opensamlResolver = new OpenSamlMetadataResolver();
        return opensamlResolver.resolve(relyingPartyRegistration);
    }
}
