package it.smartcommunitylab.aac.spid.setup;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * Utility class for mocking SPID Identity Providers (IdPs) using WireMock.
 * Centralizes the test infrastructure by providing hardcoded cryptographic materials (RSA keys and X.509 certificates),
 * SAML 2.0 XML response templates (for both successful authentications and AgID-specific anomaly errors),
 * and dynamic generation of IdP Metadata for testing HTTP-Redirect and HTTP-POST bindings.
 */
public class MockIdpSpid {

    /* =========================================================================
     * Identity Provider (IdP) Identification
     * ========================================================================= */

    protected final String SSO_PATH = "/auth/spid/sso/";
    protected final String SLO_PATH = "/auth/spid/slo/";
    public final String ASSERTING_PARTY_ENTITY_ID_REDIRECT = "https://idp.identityserver.redirect";
    public final String ASSERTING_PARTY_ENTITY_ID_POST = "https://idp.identityserver.post";

    protected boolean ASSERTING_PARTY_WANT_AUTHN_SIGNED = true;
    protected final String ASSERTING_PARTY_KEY_USAGE = "signing";

    /**
     * Shared Cryptographic Materials (PKCS#8 Keys & Certs)
     * {@link it.smartcommunitylab.aac.spid.SpidCheckLoadBootStrapTest} for the certificate and private key generation command
     */

    public final String IDP_MOCK_PRIVATE_KEY = """
        MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC6N2rhDdh2NaYf
        UtOi0PbXN2tiUZzs2/fbFhLCMjThmUn0ZOCIfWOF0ITYtELNN86d3knpd/KXW4mH
        6qCH4r88sowhn/FoAK5F5BzbAuOYTcEfV4S4DkVqk1P+T1jzeJD7fvC+90kGvRMj
        kQVOt9et7ydg64LDuXDpJBJiKehsMNn9SUQZKv8VbkFmfxXp2jBMSXA06oFwuxm/
        JKGdcwjgLI98CmKyLyPbDYAEoo0+sC8W+oOYdn76GqP2hsabm3iEUo1K9WBXveHG
        SE4B0TisIxKwwS/TNWkanGBuXuqm+fngcqWKK1BMxia+ZxLbvdXrvyvkuLjIhrjJ
        zRcNuf1JAgMBAAECggEAGDBzse62VCCtnCW1DSd/hmm8tBsC5ffAT2rOHqtz7WxC
        oZHAYv/av5AHJGPsxjNTeR6zfjWRGIfmVSFYH16ygHl3CjUjIDiaasTHhMxFcUwv
        DoRes/lSm2tY6VDjA/mA59J6W8F2mylQxPl6dYUcagvbaH50pY+/XYxMgQji7WQF
        sQKQwGgplbqzVn/W+hotuWQzcLirawwqC+MDMc4p1EqxBDICrWGdguwfJNWQoALJ
        9V7lG53ZX/eSeAXpsIbaAeWKw54/V/ZOGdDnHqdhZzh2lHT+kvWfsSN2jE1aBski
        3yVEorT+J5zOiftKxZNf8YCx3hsybdpyRply8L8fgwKBgQDk01ho2I0IV6TdcORQ
        8xjl7lJoa1dnfp0nMAGz4UqKggD7a/HBQGALQ8IRpgE2UtNIpqFbYt+Zn51k8Zup
        DhhO3LepR9EmEyZe3SEqLll9GN6YCC8l6WJK3XF+nCxWULDMF2nI8oQH0zpSmGur
        BoYCxxBFTJ6dlMkgK58UrP/p3wKBgQDQVLBtJJ0KuhG/3E84uBzBwdF06FlbDuHq
        9aq/R9pCe5BnA8qUe3AdZCZYtGv1qMHDDjIZtGfKMycidC9XHZoFTSUP+KqZ8BAH
        gB6bURWmMluDujCbh2VoyEBluY/3HIJqFZdilG1OI9hGWsgqujmRfeeophKz5DYd
        +1AmFhvN1wKBgCbbgOLlCyYEhBmahxr7/RlmnBXhTIllpdg2vcNHGbplkzcewIH5
        pZWkHvuSPhh0fi6TJUl4g9H5mee/Y5iUrSoPLx0O9gRKMjTfxjb8gfPNWldk5GTC
        ug9OhWxjpt/NegheXXdjP2p4wymtenMje3RTS38JINJPpsvQvIXtTtPvAoGADrB6
        BCgZvqDiEYIqP9iThoHxD+o2KrqA6X1K/dPGKvvlca4Nwax2ekwOfCC0oAy3JNbC
        Z5eV3eb/cml40Q6wRoFrBJZHCTWpG65H+jGcciyI0V/2f3DrkJjWGZYc9ZKYC3zc
        QMIwdtsGK+fIx2J8HqsfA4A6P17vBewreZQDf98CgYAuYEo1NZvdB8x75B4Q+0nS
        TjaoKf+1GzU8h0Lm+pYkb66NwiCJuUQ3dFEPg18E7bNpvnYsGSmbUnwL7H+klKIB
        H3w4Eo+6k3sSjHnsT+ztG++ksCSrwx8K1Pxn4TEMmc0fIxeU30m+rOLDc3hW0ijF
        j5ePfdDkG3IEKOr2Gqsp9Q==
        """;

    public final String IDP_MOCK_CERTIFICATE = """
        MIIDGjCCAgKgAwIBAgIUKtQ7+zSJ+cirIifmju+E2aAk7tgwDQYJKoZIhvcNAQEL
        BQAwMDELMAkGA1UEBhMCSVQxDTALBgNVBAoMBFRlc3QxEjAQBgNVBAMMCWxvY2Fs
        aG9zdDAeFw0yNjA1MjkxMjQ5NDhaFw0zNjA1MjYxMjQ5NDhaMDAxCzAJBgNVBAYT
        AklUMQ0wCwYDVQQKDARUZXN0MRIwEAYDVQQDDAlsb2NhbGhvc3QwggEiMA0GCSqG
        SIb3DQEBAQUAA4IBDwAwggEKAoIBAQC6N2rhDdh2NaYfUtOi0PbXN2tiUZzs2/fb
        FhLCMjThmUn0ZOCIfWOF0ITYtELNN86d3knpd/KXW4mH6qCH4r88sowhn/FoAK5F
        5BzbAuOYTcEfV4S4DkVqk1P+T1jzeJD7fvC+90kGvRMjkQVOt9et7ydg64LDuXDp
        JBJiKehsMNn9SUQZKv8VbkFmfxXp2jBMSXA06oFwuxm/JKGdcwjgLI98CmKyLyPb
        DYAEoo0+sC8W+oOYdn76GqP2hsabm3iEUo1K9WBXveHGSE4B0TisIxKwwS/TNWka
        nGBuXuqm+fngcqWKK1BMxia+ZxLbvdXrvyvkuLjIhrjJzRcNuf1JAgMBAAGjLDAq
        MAkGA1UdEwQCMAAwHQYDVR0OBBYEFPaLPw/zz4vtQBFoZncG2ROFRIxSMA0GCSqG
        SIb3DQEBCwUAA4IBAQCDzBQwG6zDGSTwve4/IMyzfdOzQ9qH11V2R6HJ5S6iVCJD
        F52lJCO/Izfs7uv5Fi5v+b5hvfnSFQR/hnPHEpev1TYHarTHvlP8aNVSYDoDoWNv
        rEGmtTog56dvzMSAqIfmuSP/VUhBhBZPJd5AulQ5eZrXXK4GkKHdyQHymvPSOsfa
        ahEmwICCaWJdFcCF37vNHkIh+rwqy8jhX7vEhlyP1n80PggnYHyQL7IKFDKAk94V
        vtP9Sw2idooJZzeC8S4idR3gZB2j7jbAYXi0jb8Zx2DF+lnWj5a/lr7VEBKkVGUk
        hjpUThPMX7zV1J5oav29DQ4HX0Ea7vwC30U17tQI
        """;

    /**
     * Baseline SAML Response by idp.
     * This template acts as the foundational DOM structure before dynamic properties.
     * Placeholders enclosed in {@code __XXX__} formatting are dynamically replaced
     * Placeholders enclosed in {@code _xxx or SPID_xxx} formatting are statically replaced
     * during the test execution or setup flow.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#esempio-di-response-con-assertion">
     *      Official AgID SPID Technical Rules - SAML Response & Assertion Example</a>
     */
    public String XML_RESPONSE_TEMPLATE =
        """
        <saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
                         xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
                         xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
                         Destination="__SSO_REGISTRATION_ID__"
                         ID="_response_test_id_value"
                         InResponseTo="__REQUEST_ID__"
                         IssueInstant="__ISSUE_INSTANT__"
                         Version="2.0">
            <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">__IDENTITY_PROVIDER__</saml2:Issuer>
            <saml2p:Status>
                <saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success" />
            </saml2p:Status>
            <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
                             ID="_assertion_test_id_value"
                             IssueInstant="__ISSUE_INSTANT__"
                             Version="2.0">
                <saml2:Issuer Format="urn:oasis:names:tc:SAML:2.0:nameid-format:entity">__IDENTITY_PROVIDER__</saml2:Issuer>
                <saml2:Subject>
                    <saml2:NameID Format="urn:oasis:names:tc:SAML:2.0:nameid-format:transient"
                                  NameQualifier="__IDENTITY_PROVIDER__">SPID-mock-transient-id-123456</saml2:NameID>
                    <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
                        <saml2:SubjectConfirmationData InResponseTo="__REQUEST_ID__"
                                                       NotOnOrAfter="__NOT_ON_OR_AFTER__"
                                                       Recipient="__SSO_REGISTRATION_ID__" />
                    </saml2:SubjectConfirmation>
                </saml2:Subject>
                <saml2:Conditions NotBefore="__NOT_BEFORE__"
                                  NotOnOrAfter="__NOT_ON_OR_AFTER__">
                    <saml2:AudienceRestriction>
                        <saml2:Audience>__AUDIENCE_METADATA__</saml2:Audience>
                    </saml2:AudienceRestriction>
                </saml2:Conditions>
                <saml2:AuthnStatement AuthnInstant="__AUTHN_INSTANT__">
                    <saml2:AuthnContext>
                        <saml2:AuthnContextClassRef>https://www.spid.gov.it/SpidL2</saml2:AuthnContextClassRef>
                    </saml2:AuthnContext>
                </saml2:AuthnStatement>
                <saml2:AttributeStatement>
                    <saml2:Attribute Name="name">
                        <saml2:AttributeValue xmlns:xs="http://www.w3.org/2001/XMLSchema"
                                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                              xsi:type="xs:string">__ATTRIBUTE_VALUE__</saml2:AttributeValue>
                    </saml2:Attribute>
                </saml2:AttributeStatement>
            </saml2:Assertion>
        </saml2p:Response>""";

    /**
     * Baseline SAML Response containing an error.
     * This template acts as the foundational DOM structure before dynamic properties
     * (like specific AgID error codes and timestamps) are injected.
     * Placeholders enclosed in {@code __XXX__} formatting are dynamically replaced
     * during the test execution or setup flow.
     *
     * @see <a href="https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/messaggi-errore.html">
 *          Official AgID SPID Technical Rules - Error Messages Mapping</a>
     */
    public String XML_RESPONSE_AGID_ERROR_TEMPLATE =
        """
        <saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
                         xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
                         ID="_response_test_id_value"
                         InResponseTo="__REQUEST_ID__"
                         IssueInstant="__ISSUE_INSTANT__"
                         Version="2.0"
                         Destination="__SSO_REGISTRATION_ID__">
            <saml2:Issuer>__IDENTITY_PROVIDER__</saml2:Issuer>
            <saml2p:Status>
                <saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Responder">
                    <saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:AuthnFailed" />
                </saml2p:StatusCode>
                <saml2p:StatusMessage>__ERROR_CODE__</saml2p:StatusMessage>
            </saml2p:Status>
        </saml2p:Response>""";


    /* =========================================================================
     * Identity Provider (IdP) Metadata Template & Generation
     * ========================================================================= */

    private final String ASSERTING_PARTY_METADATA_TEMPLATE =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:alg="urn:oasis:names:tc:SAML:metadata:algsupport" \
        entityID="%s" ID="_bf133aac099b99b3d81286e1a341f2d34188043a77fe15bf4bf1487dae9b2ea3">
        <md:IDPSSODescriptor WantAuthnRequestsSigned="%s" protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
        <md:SingleSignOnService Binding="%s" Location="%s"/>
        <md:SingleLogoutService Binding="%s" Location="%s"/>
        <md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</md:NameIDFormat>
        <md:KeyDescriptor use="%s">
        <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
        <ds:X509Data>
        <ds:X509Certificate>%s</ds:X509Certificate>
        </ds:X509Data>
        </ds:KeyInfo>
        </md:KeyDescriptor>
        </md:IDPSSODescriptor>
        </md:EntityDescriptor>""";


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
                IDP_MOCK_CERTIFICATE
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
                IDP_MOCK_CERTIFICATE
        );
    }

    public void prepareMockMetadata(WireMockServer mockIdPServerRedirect, WireMockServer mockIdPServerPost) {
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
