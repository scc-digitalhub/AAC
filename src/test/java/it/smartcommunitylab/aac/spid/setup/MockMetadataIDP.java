package it.smartcommunitylab.aac.spid.setup;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class MockMetadataIDP {

    /* =========================================================================
     * Identity Provider (IdP) Identification
     * ========================================================================= */

    protected final String SSO_PATH = "/auth/spid/sso/";
    protected final String SLO_PATH = "/auth/spid/slo/";
    public final String ASSERTING_PARTY_ENTITY_ID_REDIRECT = "https://idp.identityserver.redirect";
    public final String ASSERTING_PARTY_ENTITY_ID_POST = "https://idp.identityserver.post";

    protected boolean ASSERTING_PARTY_WANT_AUTHN_SIGNED = true;
    protected final String ASSERTING_PARTY_KEY_USAGE = "signing";

    /* =========================================================================
     * Shared Cryptographic Materials (PKCS#8 Keys & Certs)
     * ========================================================================= */

    public final String IDP_VERIFICATION_PRIVATE_KEY =
        "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCrnfaliMBloASt\n" +
        "LS/x7QfKC1L56h780wfnLSiriuv+6ogDziZIQc0O6BFN/y96ie2F5p67mNT8N3K5\n" +
        "jaMbBwrImAizNtUNR7MWRWAS9AuH1f5xdZbCexIjpkbSC6eJ0AEfzLyRkTWHBqVy\n" +
        "DFOad8ZscSHl80o2m8CPEa6Ynd3bsYjaYappaN9GCXsdEOUtn+gCtXk/gXosAFzN\n" +
        "kOY/tgrRWNMXkF8M0+JPbPOqnvbXsjoA+Ki2EPMJL7/IP9Twd3lYAYJ+MIb9A8Ll\n" +
        "dbpf/zZTOAtCKEoEeP0TCKHkExN9bAFEBIniwmi+6S0ttw2YsIWSHbUa+95D+q0C\n" +
        "kESO3GcnAgMBAAECggEAC540AQd9pDjtrXOZAqholer9AQzdGtcxJRtPh4gYwIx2\n" +
        "t3CQnY+m0X2FvmFU1izZjEO34edPc+D6aUlcuLWvVd5vqFeWm2w2hCl01D5P5trm\n" +
        "e3PZyEILUP0utNredVvu14zwBG1vfDvySK2WSKdseBGTQ+fxJuVAZ0Rr13Ud0djZ\n" +
        "of6l1dPGMdXU6Wy2aTGl7T/EoC62fTAnNY9y1oVuW3Syl51Jt5iZbIR/aVYWx5mH\n" +
        "PSNZBgw6jfkbJHIMnMnh16mRRQD1e169vhSuEDRzjamq0BXGWr+HeqjL32alI6gO\n" +
        "0NMOh5XIiXqseq9SvFA4kHdufOT+/VTNUkpzhIxf8QKBgQDxCs1wr78TUxWbSUjG\n" +
        "VmyryF/q74LN4U/402MSp0afzH3iDtxlbXmo7fSPxSL4jcN7uALbPtlp4rpHBnEh\n" +
        "6oSCRd0YMW+9JrKog/c86WxopxmJkuly/aa8ArwP0mJ4xcft8VM4vgOdw38kqhS2\n" +
        "oPNTTGuEuWlbj8pYqnBDzikyDQKBgQC2REXM6YoFeu7ecmd2KrWTJpRhWNeNLWq0\n" +
        "C1YAfRHGwDUHexybNgyTozcnW8L/TpHCBC9q1PdqGHAZTpshaiC+R+pGm4826Vhi\n" +
        "2CQy8Hwy1bV2nsk4UZvFdRFM2uHtQDUvd/mOsVNFCwAil/TPAblMi1J3OBiJkhSO\n" +
        "jyV58azVAwKBgAsscwWMQBFHQrMmHIeFLhhwe3HKlIeysCBavDb7Jhz2P8eg5LqW\n" +
        "7pLUJQgdHVfkSnGLwCYlrbJo3jW4qLnnwyi+0Fb0w7dC+fkx1N4v++SGCnsEImpA\n" +
        "M+B3R/x7xjDPCkuPakoxFL3VeClc8QTeRSlRW5KVfbrO2ZRuojGiduppAoGAEGUN\n" +
        "vkPXhWysZdf2lHt8/7J9sE/0e591NKK8ZqjZW35Yhsa9KPzwnqsUv/aSELL5i3Ei\n" +
        "7sIfSyzNkIkwjQ3lyhff69/8Pt04dROqFebp1QzCGNxpjyZQE6/XEYmyvsuCvTVW\n" +
        "fk5XBiPaLEJs493s1ATIMy8Zje5U6QnZPiHOAQ8CgYAijpZW3Fz1cOs3/eMy3R5S\n" +
        "CssFEnduMnYQz14xc9vDJnOq8Xyruus8/XZ8HVKY6cBIB/SZWO9eFixCYh5XlV8K\n" +
        "WgfOl9zfrhNADBWsavSjVlyssxKIWJkg1adNq36p7dtGKkkDDE/HcMKwRKi5TzZx\n" +
        "VCcWUmNSFqEM0cm4MMRNfg==";

    public final String IDP_VERIFICATION_CERTIFICATE =
        "MIIDSTCCAjGgAwIBAgIUHl/XG9lSuDNExvxUnjyIGwhbybIwDQYJKoZIhvcNAQEL\n" +
        "BQAwNDELMAkGA1UEBhMCSVQxETAPBgNVBAoMCFRlc3QgSWRQMRIwEAYDVQQDDAls\n" +
        "b2NhbGhvc3QwHhcNMjYwMzE5MDk0NDMyWhcNMzYwMzE2MDk0NDMyWjA0MQswCQYD\n" +
        "VQQGEwJJVDERMA8GA1UECgwIVGVzdCBJZFAxEjAQBgNVBAMMCWxvY2FsaG9zdDCC\n" +
        "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKud9qWIwGWgBK0tL/HtB8oL\n" +
        "UvnqHvzTB+ctKKuK6/7qiAPOJkhBzQ7oEU3/L3qJ7YXmnruY1Pw3crmNoxsHCsiY\n" +
        "CLM21Q1HsxZFYBL0C4fV/nF1lsJ7EiOmRtILp4nQAR/MvJGRNYcGpXIMU5p3xmxx\n" +
        "IeXzSjabwI8Rrpid3duxiNphqmlo30YJex0Q5S2f6AK1eT+BeiwAXM2Q5j+2CtFY\n" +
        "0xeQXwzT4k9s86qe9teyOgD4qLYQ8wkvv8g/1PB3eVgBgn4whv0DwuV1ul//NlM4\n" +
        "C0IoSgR4/RMIoeQTE31sAUQEieLCaL7pLS23DZiwhZIdtRr73kP6rQKQRI7cZycC\n" +
        "AwEAAaNTMFEwHQYDVR0OBBYEFHrVg5fygJ7DtkvNhxZ5cBdmsBSnMB8GA1UdIwQY\n" +
        "MBaAFHrVg5fygJ7DtkvNhxZ5cBdmsBSnMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZI\n" +
        "hvcNAQELBQADggEBAA0W4zRUYuWLCpJe7JqkijeFVu+ZlXVl/btC0GQz7S8dwLmp\n" +
        "qVLs9GpH+MjQVEsl/M8D1scBCUmCJZ8srHvDDyhw1j4RMoqipUo98FsI8wU2Oiyy\n" +
        "LUBQW3gJQehvkorcHnV/AQBXoSp/FKhtLjRqMNXTCj7b9Ebjj//MtLaLWxpzoxrF\n" +
        "0kLHq7uF9FlabsS+nv1oeGgyvktlzn0QabNrOyn7AlJc67bh4eKqpHoNukEqc93c\n" +
        "Zik3hdbJ56VPoKbr7lLz+rsNbKwq0dIfcRJUOGgEK4fs1MLDAMoJgiCf8iN8YnaK\n" +
        "Y6QpmHEn6w3QEC+HDJpkxAbHo4yrI1JV8gQAZEA=";

    /**
     * Baseline SAML Response by idp.
     * This template acts as the foundational DOM structure before dynamic properties.
     */
    public String XML_RESPONSE_TEMPLATE =
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
    public String XML_RESPONSE_AGID_ERROR_TEMPLATE =
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


    /* =========================================================================
     * Identity Provider (IdP) Metadata Template & Generation
     * ========================================================================= */

    private final String ASSERTING_PARTY_METADATA_TEMPLATE =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:alg=\"urn:oasis:names:tc:SAML:metadata:algsupport\" " +
        "entityID=\"%s\" ID=\"_bf133aac099b99b3d81286e1a341f2d34188043a77fe15bf4bf1487dae9b2ea3\">\n" +
        "<md:IDPSSODescriptor WantAuthnRequestsSigned=\"%s\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n" +
        "<md:SingleSignOnService Binding=\"%s\" Location=\"%s\"/>\n" +
        "<md:SingleLogoutService Binding=\"%s\" Location=\"%s\"/>\n" +
        "<md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</md:NameIDFormat>\n" +
        "<md:KeyDescriptor use=\"%s\">\n" +
        "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
        "<ds:X509Data>\n" +
        "<ds:X509Certificate>%s</ds:X509Certificate>\n" +
        "</ds:X509Data>\n" +
        "</ds:KeyInfo>\n" +
        "</md:KeyDescriptor>\n" +
        "</md:IDPSSODescriptor>\n" +
        "</md:EntityDescriptor>";


    private String getMetadataRedirect() {
        return String.format(
            ASSERTING_PARTY_METADATA_TEMPLATE,
            ASSERTING_PARTY_ENTITY_ID_REDIRECT,
            ASSERTING_PARTY_WANT_AUTHN_SIGNED,
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect", // SSO Binding
            ASSERTING_PARTY_ENTITY_ID_REDIRECT + SSO_PATH,
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect", // SLO Binding
            ASSERTING_PARTY_ENTITY_ID_REDIRECT + SLO_PATH,
            ASSERTING_PARTY_KEY_USAGE,
            IDP_VERIFICATION_CERTIFICATE
        );
    }

    private String getMetadataPost() {
        return String.format(
            ASSERTING_PARTY_METADATA_TEMPLATE,
            ASSERTING_PARTY_ENTITY_ID_POST,
            ASSERTING_PARTY_WANT_AUTHN_SIGNED,
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST",     // SSO Binding
            ASSERTING_PARTY_ENTITY_ID_POST + SSO_PATH,
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST",     // SLO Binding
            ASSERTING_PARTY_ENTITY_ID_POST + SLO_PATH,
            ASSERTING_PARTY_KEY_USAGE,
            IDP_VERIFICATION_CERTIFICATE
        );
    }

    public void preprareMockMetadata(WireMockServer mockIdPServerRedirect, WireMockServer mockIdPServerPost) {
        // CONFIGURE IDP SERVER REDIRECT
        mockIdPServerRedirect.stubFor(
            WireMock.get(urlEqualTo("/metadata"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/xml")
                    .withBody(getMetadataRedirect()))
        );

        // CONFIGURE IDP SERVER POST
        mockIdPServerPost.stubFor(
            WireMock.get(urlEqualTo("/metadata"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/xml")
                    .withBody(getMetadataPost()))
        );
    }
}
