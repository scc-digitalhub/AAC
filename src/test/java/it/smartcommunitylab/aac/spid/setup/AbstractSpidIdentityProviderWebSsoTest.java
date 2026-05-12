package it.smartcommunitylab.aac.spid.setup;

import it.smartcommunitylab.aac.dto.RealmConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Setup class for SPID Authentication Flow testing.
 * Specific for simulating and validating the SAML 2.0 Web SSO exchange.
 */
public abstract class AbstractSpidIdentityProviderWebSsoTest extends BaseSpidTest{

    /* =========================================================================
     * Service Provider (SP)
     * ========================================================================= */

    protected String signingSecondIdpProvider;
    protected String signingSecondIdpMetadataUrl;
    protected String signingSecondIdpSsoUrl;
    protected String signingSecondIdpEntityId;
    protected Boolean signingSecondUseAssertionConsumerServiceUrl;
    protected Integer signingSecondAttributeConsumingServiceIndex;
    protected Set<SpidAttribute> signingSecondSetSpidAttributes;

    protected String registrationSecondIdRedirect;
    protected String registrationSecondIdPost;

    /* =========================================================================
     * Web SSO Specific Destinations and Actions
     * ========================================================================= */

    protected final String AUTHENTICATE_PATH = "/auth/spid/authenticate/";
    protected final String USER_DESTINATION_URL = BASE_URL + "/console/user";
    protected final String LOGIN_DESTINATION_URL = BASE_URL + "/-/spid-test/login";

    /* =========================================================================
     * SAML Mock Payloads
     * ========================================================================= */
    protected MockMetadataIDP mockMetadataIDP = new MockMetadataIDP();

    protected void initSecondReamlByBoostrap(RealmConfig realm) {
        List<ConfigurableIdentityProvider> idps = realm.getIdentityProviders();
        assertThat(idps.size()).isEqualTo(2);
        ConfigurableIdentityProvider idp2 = idps.get(1);
        assertThat(idp2.getProvider()).isNotNull();

        signingSecondIdpProvider = idp2.getProvider();
        signingSecondIdpMetadataUrl = BASE_URL + METADATA_PATH + encodeRegistrationId(signingSecondIdpProvider);
        signingSecondIdpSsoUrl = BASE_URL + SSO_PATH + encodeRegistrationId(signingSecondIdpProvider);
        signingSecondIdpEntityId = BASE_URL + METADATA_PATH + encodeRegistrationId(signingSecondIdpProvider);

        SpidIdentityProviderConfigMap configmapSecond = new SpidIdentityProviderConfigMap();
        configmapSecond.setConfiguration(idp2.getConfiguration());

        assertThat(configmapSecond.getSpidAttributes()).isNotNull();
        assertThat(configmapSecond.getUseAssertionConsumerServiceUrl()).isNotNull();
        assertThat(configmapSecond.getAttributeConsumingServiceIndex()).isNotNull();

        signingSecondSetSpidAttributes = configmapSecond.getSpidAttributes();
        signingSecondUseAssertionConsumerServiceUrl = configmapSecond.getUseAssertionConsumerServiceUrl();
        signingSecondAttributeConsumingServiceIndex = configmapSecond.getAttributeConsumingServiceIndex();
    }

    protected void initSecondRegistrationIdBinding(String ASSERTING_PARTY_ENTITY_ID_REDIRECT, String ASSERTING_PARTY_ENTITY_ID_POST){
        registrationSecondIdRedirect = encodeRegistrationId(signingSecondIdpProvider + "|" + ASSERTING_PARTY_ENTITY_ID_REDIRECT);
        registrationSecondIdPost = encodeRegistrationId(signingSecondIdpProvider + "|" + ASSERTING_PARTY_ENTITY_ID_POST);
    }

    /**
     * Baseline SAML Response by idp.
     * This template acts as the foundational DOM structure before dynamic properties.
     */
    protected String XML_RESPONSE_TEMPLATE =
            "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "                 xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "                 xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"\n" +
            "                 Destination=\"http://localhost:8080/auth/spid/sso/registrationId\"\n" +
            "                 ID=\"_125f62d9-b3d3-47a8-8c5b-6a4761483d96\"\n" +
            "                 InResponseTo=\"ARQ4f2b078-5433-464c-9c4d-0702804f7195\"\n" +
            "                 IssueInstant=\"2025-09-23T09:19:10.491Z\"\n" +
            "                 Version=\"2.0\">\n" +
            "    <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">https://idp.identityserver.invalid</saml2:Issuer>\n" +
            "    <Signature xmlns=\"http://www.w3.org/2000/09/xml_sig#\">\n" +
            "        <SignedInfo>\n" +
            "            <CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" />\n" +
            "            <SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" />\n" +
            "            <Reference URI=\"#_125f62d9-b3d3-47a8-8c5b-6a4761483d96\">\n" +
            "                <Transforms>\n" +
            "                    <Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" />\n" +
            "                    <Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" />\n" +
            "                </Transforms>\n" +
            "                <DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" />\n" +
            "                <DigestValue>JJomN0gczJW8ectgSjm5NAnzU6YYBylqr853vyqo+F4=</DigestValue>\n" +
            "            </Reference>\n" +
            "        </SignedInfo>\n" +
            "        <SignatureValue>...</SignatureValue>\n" +
            "        <KeyInfo>\n" +
            "            <X509Data>\n" +
            "                <X509Certificate>...</X509Certificate>\n" +
            "            </X509Data>\n" +
            "        </KeyInfo>\n" +
            "    </Signature>\n" +
            "    <saml2p:Status>\n" +
            "        <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\" />\n" +
            "    </saml2p:Status>\n" +
            "    <saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "                     ID=\"_5aff0ae5-b528-4adb-84e2-4fb6219a1749\"\n" +
            "                     IssueInstant=\"2025-09-23T09:19:09.491Z\"\n" +
            "                     Version=\"2.0\">\n" +
            "        <saml2:Issuer Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:entity\">https://idp.identityserver.invalid</saml2:Issuer>\n" +
            "        <Signature xmlns=\"http://www.w3.org/2000/09/xml_sig#\">\n" +
            "            <SignedInfo>\n" +
            "                <CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" />\n" +
            "                <SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" />\n" +
            "                <Reference URI=\"#_5aff0ae5-b528-4adb-84e2-4fb6219a1749\">\n" +
            "                    <Transforms>\n" +
            "                        <Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" />\n" +
            "                        <Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" />\n" +
            "                    </Transforms>\n" +
            "                    <DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" />\n" +
            "                    <DigestValue>ZxExmdALZ4lJwhadILmGCwHn/nyBwxm1mC6cb3aykns=</DigestValue>\n" +
            "                </Reference>\n" +
            "            </SignedInfo>\n" +
            "            <SignatureValue>...</SignatureValue>\n" +
            "            <KeyInfo>\n" +
            "                <X509Data>\n" +
            "                    <X509Certificate>...</X509Certificate>\n" +
            "                </X509Data>\n" +
            "            </KeyInfo>\n" +
            "        </Signature>\n" +
            "        <saml2:Subject>\n" +
            "            <saml2:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\"\n" +
            "                          NameQualifier=\"https://idp.identityserver.invalid\">SPID-a8c0c2e0-687b-4324-a4ba-1759d453397c</saml2:NameID>\n" +
            "            <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
            "                <saml2:SubjectConfirmationData InResponseTo=\"ARQ4f2b078-5433-464c-9c4d-0702804f7195\"\n" +
            "                                               NotOnOrAfter=\"2025-09-23T09:20:09.491Z\"\n" +
            "                                               Recipient=\"http://localhost:8080/auth/spid/sso/registrationId\" />\n" +
            "            </saml2:SubjectConfirmation>\n" +
            "        </saml2:Subject>\n" +
            "        <saml2:Conditions NotBefore=\"2025-09-23T09:19:09.491Z\"\n" +
            "                          NotOnOrAfter=\"2025-09-23T09:20:09.491Z\">\n" +
            "            <saml2:AudienceRestriction>\n" +
            "                <saml2:Audience>http://localhost:8080/icar-lp/metadata</saml2:Audience>\n" +
            "            </saml2:AudienceRestriction>\n" +
            "        </saml2:Conditions>\n" +
            "        <saml2:AuthnStatement AuthnInstant=\"2025-09-23T09:19:09.491Z\">\n" +
            "            <saml2:AuthnContext>\n" +
            "                <saml2:AuthnContextClassRef>https://www.spid.gov.it/SpidL2</saml2:AuthnContextClassRef>\n" +
            "            </saml2:AuthnContext>\n" +
            "        </saml2:AuthnStatement>\n" +
            "        <saml2:AttributeStatement>\n" +
            "            <saml2:Attribute Name=\"name\">\n" +
            "                <saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "                                      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "                                      xsi:type=\"xs:string\">ROSSI</saml2:AttributeValue>\n" +
            "            </saml2:Attribute>\n" +
            "        </saml2:AttributeStatement>\n" +
            "    </saml2:Assertion>\n" +
            "</saml2p:Response>";

    /**
     * Baseline SAML Response containing an error.
     * This template acts as the foundational DOM structure before dynamic properties
     * (like specific AgID error codes and timestamps) are injected.
     */
    protected String XML_RESPONSE_AGID_ERROR_TEMPLATE =
            "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "                 xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "                 ID=\"_9494dc2f8897e3b64d6adcd54e8c695a\"\n" +
            "                 InResponseTo=\"s26cd2718a765d08ecd765551b7405b20d02ee65\"\n" +
            "                 IssueInstant=\"2026-03-09T09:31:34.144Z\"\n" +
            "                 Version=\"2.0\"\n" +
            "                 Destination=\"http://localhost:8080/icar-lp/AssertionConsumerServiceProxy\">\n" +
            "    <saml2:Issuer>https://idp.identityserver.invalid</saml2:Issuer>\n" +
            "    <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "        <ds:SignedInfo>\n" +
            "            <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" />\n" +
            "            <ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" />\n" +
            "            <ds:Reference URI=\"#_9494dc2f8897e3b64d6adcd54e8c695a\">\n" +
            "                <ds:Transforms>\n" +
            "                    <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" />\n" +
            "                    <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" />\n" +
            "                </ds:Transforms>\n" +
            "                <ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" />\n" +
            "                <ds:DigestValue>SBXL44s+ZiyMX2yTGJ6ehDJHCUmgJWhN39nOOs+zoFw=</ds:DigestValue>\n" +
            "            </ds:Reference>\n" +
            "        </ds:SignedInfo>\n" +
            "        <ds:SignatureValue>lUs8o+DCANcUI9FQSW7x3..mrcIaA==</ds:SignatureValue>\n" +
            "        <ds:KeyInfo>\n" +
            "            <ds:X509Data>\n" +
            "                <ds:X509Certificate>MIIDDCCA+SgAwI...TwIhEXyzknoiw1mGIEWZc6scnOAiwZeqTccTYVNHp+PFs9SD8l+2PO4Oh8Y3dYT+5ojv+S6T7vy5xE=</ds:X509Certificate>\n" +
            "            </ds:X509Data>\n" +
            "        </ds:KeyInfo>\n" +
            "    </ds:Signature>\n" +
            "    <saml2p:Status>\n" +
            "        <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Responder\">\n" +
            "            <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:AuthnFailed\" />\n" +
            "        </saml2p:StatusCode>\n" +
            "        <saml2p:StatusMessage>ErrorCode nr25</saml2p:StatusMessage>\n" +
            "    </saml2p:Status>\n" +
            "</saml2p:Response>";
}
