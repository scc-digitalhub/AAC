package it.smartcommunitylab.aac.saml;

import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;

import net.shibboleth.utilities.java.support.xml.ParserPool;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.impl.AuthnRequestImpl;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.Inflater;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SamlRequestRedirectBindingTest {

    static {
        OpenSamlInitializationService.initialize();
    }


    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BootstrapConfig config;


    private final String BASE_URL = "http://localhost";
    private final String REQUEST_PATH_FORMAT = "/auth/%s/authenticate/%s"; // NOTE. format is hardcoded and not retrievable with a method
    private final String CONSUMER_URL_FORMAT = "http://localhost/auth/%s/sso/%s";
    private final String ISSUER_URL_FORMAT = "http://localhost/auth/%s/metadata/%s";
    private final Charset URL_CHARSET = StandardCharsets.ISO_8859_1;
    private final XMLObjectProviderRegistry registry = ConfigurationService.get(XMLObjectProviderRegistry.class);
    private final ParserPool parserPool = registry.getParserPool();
    private final String accpetedSignatureAlgorithm = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256; // matches with java security standard name SHA256WithRSA TODO: find a library that performs the matching
    private final String SIGNATURE_ALGO_STD_NAME = "SHA256WithRSA"; // TODO: find a library that maps the saml signature name format () to the java security name format (https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html)

    private MockMvc mockMvc;
    private String consumerUrl;
    private String issuerUrl;
    private String samlRequestUrl;
    private String samlWebSsoUrl;
    private String samlWebSsoBinding;
    private boolean forceAuthn;
    private boolean nameIdAllowCreate;
    private String nameIdFormat;
    private String signingCertificate;
    private Set<String> redirectUriQueryKeys;
    private Set<String> requestAuthnContextClassRef;


    @BeforeEach
    public void readBootstrapConfiguration() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        if (redirectUriQueryKeys != null) {
            redirectUriQueryKeys.clear(); // set is refreshed at each step
        } else {
            redirectUriQueryKeys = new HashSet<>();
        }
        redirectUriQueryKeys.add("SAMLRequest");
        redirectUriQueryKeys.add("RelayState");

        config.getRealms().forEach(realmConfig -> {
            if (realmConfig.getRealm().getSlug().equals("saml-test")) {
                List<ConfigurableIdentityProvider> idps = realmConfig.getIdentityProviders();
                assertThat(idps.size()).isEqualTo(2); // sanity check,

                ConfigurableIdentityProvider cfgIdP = idps.get(0);
                consumerUrl = String.format(CONSUMER_URL_FORMAT, cfgIdP.getAuthority(), cfgIdP.getProvider());
                issuerUrl = String.format(ISSUER_URL_FORMAT, cfgIdP.getAuthority(), cfgIdP.getProvider());
                samlRequestUrl = BASE_URL + String.format(REQUEST_PATH_FORMAT, cfgIdP.getAuthority(), cfgIdP.getProvider());
                samlWebSsoUrl = cfgIdP.getConfiguration().get("webSsoUrl").toString();
                samlWebSsoBinding = cfgIdP.getConfiguration().get("ssoServiceBinding").toString();
                assertThat(samlWebSsoBinding).isEqualTo("HTTP-Redirect");
                signingCertificate = cfgIdP.getConfiguration().get("signingCertificate").toString();
                Set<String> validWebSsoBindings = new HashSet<>(){{add("HTTP-POST"); add("HTTP-Redirect");}};
                assertThat(validWebSsoBindings.contains(samlWebSsoBinding)).isTrue();
                if ((Boolean) cfgIdP.getConfiguration().get("signAuthNRequest")) {
                    redirectUriQueryKeys.add("SigAlg");
                    redirectUriQueryKeys.add("Signature");
                }
                requestAuthnContextClassRef = new HashSet<>();
                assertTrue(cfgIdP.getConfiguration().get("authnContextClasses") instanceof ArrayList<?>);
                for (Object value : (ArrayList<?>) cfgIdP.getConfiguration().get("authnContextClasses")) {
                    requestAuthnContextClassRef.add(value.toString());
                }

                forceAuthn = (boolean)cfgIdP.getConfiguration().get("forceAuthn");
                nameIdAllowCreate = (boolean)cfgIdP.getConfiguration().get("nameIDAllowCreate"); // true
                nameIdFormat = cfgIdP.getConfiguration().get("nameIDFormat").toString(); // urn:oasis:names:tc:SAML:2.0:nameid-format:transient
                cfgIdP.getConfiguration().get("trustEmailAddress"); // true

                // TODO: valuta altre configurazioni necessarie per saml-test
            }
        });
    }

    /**
     * check that a saml request is indeed set and that it returns a matching return status
     */
    @Test
    public void requestIsSentTest() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(samlRequestUrl);
        ResultMatcher expectedStatus = status().isFound();
        MvcResult res = this.mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(expectedStatus)
                .andReturn();
        assertThat(res.getResponse()).isNotNull();
    }

    /**
     * check that the request is created and that is return a redirect URI with matching "structure"
     */
    @Test
    public void requestIsCreatedTest() throws Exception {
//        assertThat(samlWebSsoBinding).isEqualTo("HTTP-Redirect");
        MockHttpServletRequestBuilder requestBuilder = get(samlRequestUrl);
        MvcResult res = this.mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isFound())
                .andReturn();

        String rawRedirectUrl = res.getResponse().getRedirectedUrl();
        assertThat(rawRedirectUrl).isNotNull();
        assertThat(rawRedirectUrl).isNotBlank();
        URL redirectUrl = new URL(rawRedirectUrl);
        String observedWebSsoUrl = String.format("%s://%s%s", redirectUrl.getProtocol(), redirectUrl.getAuthority(), redirectUrl.getPath());
        assertThat(observedWebSsoUrl).isEqualTo(samlWebSsoUrl);
        MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(rawRedirectUrl).build().getQueryParams();
        Set<String> obtainedQueryKeys = parameters.keySet();
        assertThat(obtainedQueryKeys).isEqualTo(redirectUriQueryKeys);
    }


    /**
     * check that the redirect URI returned fromt he request contains a well formed XML
     */
    @Test
    public void requestIsWellFormedTest() throws Exception {
//        assertThat(samlWebSsoBinding).isEqualTo("HTTP-Redirect");
        MockHttpServletRequestBuilder requestBuilder = get(samlRequestUrl);
        MvcResult res = this.mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isFound())
                .andReturn();

        String rawRedirectUrl = res.getResponse().getRedirectedUrl();
        assertThat(rawRedirectUrl).isNotNull();
        MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(rawRedirectUrl).build().getQueryParams();
        assertThat(parameters.get("SAMLRequest")).isNotNull();
        assertThat(parameters.get("SAMLRequest").size()).isGreaterThan(0);
        String rawUrlB64EncodedCompressedSamlRequest = parameters.get("SAMLRequest").get(0);

        // manually extract information from the build the SAML Request
        String rawB64EncodedCompressedSamlRequest = UriUtils.decode(rawUrlB64EncodedCompressedSamlRequest, URL_CHARSET);
        byte[] compressedSamlRequest = Base64.getDecoder().decode(rawB64EncodedCompressedSamlRequest);
        byte[] inflatedRequestBuffer = new byte[(10*compressedSamlRequest.length)];
        Inflater decompressingAlgorithm = new Inflater(true);
        decompressingAlgorithm.setInput(compressedSamlRequest, 0, compressedSamlRequest.length);
        int inflatedRequestLength = decompressingAlgorithm.inflate(inflatedRequestBuffer);
        decompressingAlgorithm.end();
        String rawSamlRequest = new String(inflatedRequestBuffer, 0, inflatedRequestLength);

        // parse the SAML Request as xml
        Document document = parserPool.parse(new ByteArrayInputStream(rawSamlRequest.getBytes()));
        Element element = document.getDocumentElement();

        Unmarshaller unmarshaller = registry.getUnmarshallerFactory().getUnmarshaller(element);
        assertThat(unmarshaller).isNotNull();

        assertDoesNotThrow(() -> {
            unmarshaller.unmarshall(element);
        });
//        XMLObject xmlObject = unmarshaller.unmarshall(element);
    }

    /**
     * check that the redirect URI returned fromt the request contains a correct XML
     */
    @Test
    public void requestSamlIsCorrectTest() throws Exception {
//        assertThat(samlWebSsoBinding).isEqualTo("HTTP-Redirect");
        MockHttpServletRequestBuilder requestBuilder = get(samlRequestUrl);
        MvcResult res = this.mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isFound())
                .andReturn();

        String rawRedirectUrl = res.getResponse().getRedirectedUrl();
        assertThat(rawRedirectUrl).isNotNull();
        MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(rawRedirectUrl).build().getQueryParams();
        assertThat(parameters.get("SAMLRequest")).isNotNull();
        assertThat(parameters.get("SAMLRequest").size()).isGreaterThan(0);
        String rawUrlB64EncodedCompressedSamlRequest = parameters.get("SAMLRequest").get(0);

        // manually extract information from the build the SAML Request
        String rawB64EncodedCompressedSamlRequest = UriUtils.decode(rawUrlB64EncodedCompressedSamlRequest, URL_CHARSET);
        byte[] compressedSamlRequest = Base64.getDecoder().decode(rawB64EncodedCompressedSamlRequest);
        byte[] inflatedRequestBuffer = new byte[(10*compressedSamlRequest.length)];
        Inflater decompressingAlgorithm = new Inflater(true);
        decompressingAlgorithm.setInput(compressedSamlRequest, 0, compressedSamlRequest.length);
        int inflatedRequestLength = decompressingAlgorithm.inflate(inflatedRequestBuffer);
        decompressingAlgorithm.end();
        String rawSamlRequest = new String(inflatedRequestBuffer, 0, inflatedRequestLength);

        // parse the SAML Request as xml
        Document document = parserPool.parse(new ByteArrayInputStream(rawSamlRequest.getBytes()));
        Element element = document.getDocumentElement();

        Unmarshaller unmarshaller = registry.getUnmarshallerFactory().getUnmarshaller(element);
        assertThat(unmarshaller).isNotNull();

        assertDoesNotThrow(() -> {
            unmarshaller.unmarshall(element);
        });

        XMLObject xmlObject = unmarshaller.unmarshall(element);
        assertTrue(xmlObject instanceof AuthnRequestImpl);
        AuthnRequestImpl samlRequest = (AuthnRequestImpl) xmlObject;

        assertThat(samlRequest.isSigned()).isEqualTo(false); // we always expect false since in redirect mode the signature is "detached" as supplementary query parameters
        assertThat(samlRequest.getDestination()).isEqualTo(samlWebSsoUrl);
        assertThat(samlRequest.getAssertionConsumerServiceURL()).isEqualTo(consumerUrl);
        assertThat(samlRequest.isForceAuthn()).isEqualTo(forceAuthn);
        // TODO: check su dove si può tirare fuori l'ID ARQecb1f34-ba53-4c75-bb21-6cb759d97054

        assertThat(samlRequest.getIssuer().getValue()).isEqualTo(issuerUrl);
        Set<String> obtainedAuthnContextClassRefs = new HashSet<>();
        for (AuthnContextClassRef ref : samlRequest.getRequestedAuthnContext().getAuthnContextClassRefs()) {
            obtainedAuthnContextClassRefs.add(ref.getAuthnContextClassRef());
        }
        assertThat(obtainedAuthnContextClassRefs).isEqualTo(requestAuthnContextClassRef);

        assertThat(samlRequest.getNameIDPolicy().getAllowCreate()).isEqualTo(nameIdAllowCreate);
        assertThat(samlRequest.getNameIDPolicy().getFormat()).isEqualTo(nameIdFormat);
    }

    /**
     * check that the redirect URI returned fromt the request contains a matching signature
     */
    @Test
    public void requestSamlSignatureIsCorrectTest() throws Exception {
        assertThat(samlWebSsoBinding).isEqualTo("HTTP-Redirect");
        MockHttpServletRequestBuilder requestBuilder = get(samlRequestUrl);
        MvcResult res = this.mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isFound())
                .andReturn();

        String rawRedirectUrl = res.getResponse().getRedirectedUrl();
        assertThat(rawRedirectUrl).isNotNull();
        MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUriString(rawRedirectUrl).build().getQueryParams();

        // signature verification: to verify the signature, the following are required (brief memo):
        // (1) extract which signature algorithm is used
        // (2) extract the message that was signed (hashing the message is not required, as this step is done by the algorithm)
        // (3) extract the signature value
        // (4) extract the public key
        // (5) use the public key to check that the signature (of that message), with the known algorithm, is indeed valid

        // signature algorithm
        assertThat(parameters.get("SigAlg")).isNotNull();
        assertThat(parameters.get("SigAlg").size()).isGreaterThan(0);
        String rawUriSignatureAlgorithm = parameters.get("SigAlg").get(0);
        String obtainedSignatureAlgorithm = UriUtils.decode(rawUriSignatureAlgorithm, URL_CHARSET);
        assertThat(obtainedSignatureAlgorithm).isEqualTo(accpetedSignatureAlgorithm);

        // signature value
        assertThat(parameters.get("Signature")).isNotNull();
        assertThat(parameters.get("Signature").size()).isGreaterThan(0);
        byte[] signatureValue = Base64.getDecoder().decode(URLDecoder.decode(parameters.get("Signature").get(0), URL_CHARSET));

        // signature messagge
        String rawSamlRequestUrlEncoded = parameters.get("SAMLRequest").get(0);
        String rawRelayStateUrlEncoded = parameters.get("RelayState").get(0);
        byte[] message = String.format("SAMLRequest=%s&RelayState=%s&SigAlg=%s", rawSamlRequestUrlEncoded, rawRelayStateUrlEncoded, rawUriSignatureAlgorithm).getBytes();

        // public key (obtained from signing certificate)
        String cleanSigningCertificate = signingCertificate.replace("\n", "");
        cleanSigningCertificate = StringUtils.removeStart(cleanSigningCertificate, "-----BEGIN CERTIFICATE-----");
        cleanSigningCertificate = StringUtils.removeEnd(cleanSigningCertificate, "-----END CERTIFICATE-----");
        InputStream certificateStream =  new ByteArrayInputStream(Base64.getDecoder().decode(cleanSigningCertificate.getBytes()));
        X509Certificate signingCertificateX509 = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(certificateStream);
        PublicKey signaturePublicKey = signingCertificateX509.getPublicKey();

        // finally, verify
        Signature signatureAlgorithm = Signature.getInstance(SIGNATURE_ALGO_STD_NAME);
        signatureAlgorithm.initVerify(signaturePublicKey);
        signatureAlgorithm.update(message);
        boolean isSignatureValid = signatureAlgorithm.verify(signatureValue);
        assertThat(isSignatureValid).isTrue();

        System.out.println("stoop");
    }

}
