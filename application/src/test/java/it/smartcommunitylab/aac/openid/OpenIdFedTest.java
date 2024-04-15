package it.smartcommunitylab.aac.openid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.util.JSONArrayUtils;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.federation.api.EntityListingRequest;
import com.nimbusds.openid.connect.sdk.federation.api.EntityListingResponse;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.registration.ClientRegistrationType;
import com.nimbusds.openid.connect.sdk.federation.trust.EntityStatementRetriever;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainResolver;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainSet;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import com.nimbusds.openid.connect.sdk.rp.ApplicationType;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.oauth.model.EncryptionMethod;
import it.smartcommunitylab.aac.oauth.model.JWEAlgorithm;
import it.smartcommunitylab.aac.oauth.model.JWSAlgorithm;
import it.smartcommunitylab.aac.oauth.model.PromptMode;
import it.smartcommunitylab.aac.openidfed.auth.OpenIdFedOAuth2AuthorizationRequestResolver;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openidfed.service.DefaultOpenIdRpMetadataResolver;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// @SpringBootTest
// @AutoConfigureMockMvc
// @ActiveProfiles("test")
public class OpenIdFedTest {

    private static final String KEY =
        "    {\n" + //
        "      \"p\": \"zcbX5-tyK01M6yStrH2jsUtEUtSJrftYvEUp8cuV0ZVnsCHOGwDkLc96FH8jyXW9wjm4IkJHNdhp-w8N1vfu2Hrm3abrXSdgl7MhuPOo93gkV6ZWZF53ieiRKkyadDNNTIufMJRhoTpewLpm7JaaCjX-40aJAaZcz5gFh-sAOHc\",\n" + //
        "      \"kty\": \"RSA\",\n" + //
        "      \"q\": \"oDUPIr0Wy_sztVvvfRFrReo77lRCEr4TGRbyvFhHTQXmEIR6IzVdApI_VYg8V3yUCUNfQRK3QDVhHE83qphMjxacSNu6tdTeKO_bYVRU8JYKY17j04N-8axgl0o-qv1VhKtyp79UYrBhP1kRFMgezVVIuJ01-feieUXE6hU-S8E\",\n" + //
        "      \"d\": \"EU6rBQ0bsorWDaKEf2WA_EJeKrMB81o11ESXMqFUM6iTvbaGKLV8cfbgDVPVgbMA9YXAnr_OzTevT9Kj6X98YoCFf2mWc2eFo_80_jI2NcgPvDV5euxeVEKd_JveApRGTSIRLgSPOCzDU4RmhKMD83fVplP-8uzbZ_Sy9w9IpmQhemDE4_2nALON-6i6Id6pMRCWuk3WbN7EG6H6q726aUA0pcajv0ERQJNauKsqLRTUQ-N7-d2uC7rUuP9TtEV0J3yWmT_Ms6qQQopAFQBmPF9X1tmADQWtfRkrTf_fR7uoesNgyZ9Zh5AoSP60RccHfhaoEmq6PKPTdfBiAMNEAQ\",\n" + //
        "      \"e\": \"AQAB\",\n" + //
        "      \"use\": \"sig\",\n" + //
        "      \"kid\": \"cce6d4e8-e8a4-4ceb-a9ca-9b6336220763\",\n" + //
        "      \"qi\": \"Nz_ltI2rIyv9sx9oqUhcYsEUbarebFEDc1Cagahajow-w_iaAPTAIGxrS6u0wdAXRPyQhK8OUJTUkrEIhncsYNM9btdg75Jg0s3dVCSdTOBLYAcg4BnoCeZTy8bHmiGeReMao-Ud84tgxxp8VaCGK5xjX7_dcPeyyV2t2bwQbJU\",\n" + //
        "      \"dp\": \"okO9d21rTwgasoXuyckdLq5ahzKACwjblUK466mNT4KQDAzsONEAHbuw2b7UGoXVB_Z549H901D_aXEaibxixKRNrtV0XbWybo1b9zt0rwg3KELLd0Qi4UXaSs-zMBRQTR8Afc9UGU4s769NgBzWBV1EzDWPzhRAtZZ8Lgy61Ms\",\n" + //
        "      \"alg\": \"RS256\",\n" + //
        "      \"dq\": \"eAYoV8NIL_v7ylSdQJrrPk71JWG0uqvyTOq3YknU6SyRJzDI_FW_X-zHTEIryvOHSMVTuMHXzl_vaDElKEa_nLe40JDl-dIHuq2wMuPmWvRdxgLSNqWeVlrpZo72Ek6HYkS8OlaKVwGDad1FgcdH5FePitglFyfibm7JzwGpUUE\",\n" + //
        "      \"n\": \"gMbtRjk4fb-cQ63AWKWP-oRfPwvCzPep7MWBuKIHleEO1NFgeMgdDGt7H4Cp5b2T5TklGdJbD01TsQBXUFEWJZPhTlTOYv4rlS7_OhsZk5jsXDpLWMIv6qLTkSTQSgGnOEeQ3CB40DesTWt7Q3lj8BEBkv90sQ4ijV9qmf2qmV9cvAakfVxo74XvxHiX2Mn_Vs1xm1OQNQm9evsmDVJA_UTbGQIRRnAsAv3eot4Aok4dH2FS-pfrSGmuJlpWMAEOMYE8EkwH7OCymf5XaUFuH40FmwGvWlcMwJ5Lcm97BbS6JnDQkRlkJaAkJkXvXTC9pqmtXAnBeEX47qTGq4dutw\"\n" + //
        "    }\n";

    private static final String KEYS = "{\"keys\":[" + KEY + "]}";

    private static final String TRUST_MARK =
        "{\n" + //
        " \"id\": \"https://www.spid.gov.it/certification/rp\",\n" + //
        " \"trust_mark\":\n" + //
        "\"eyJraWQiOiJmdWtDdUtTS3hwWWJjN09lZUk3Ynlya3N5a0E1bDhPb2RFSXVyOHJoNFlBIiwidHlwIjoidHJ1c3QtbWFyaytqd3QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL3d3dy5hZ2lkLmdvdi5pdCIsInN1YiI6Imh0dHBzOi8vcnAuZXhhbXBsZS5pdC9zcGlkIiwiaWF0IjoxNTc5NjIxMTYwLCJpZCI6Imh0dHBzOi8vd3d3LnNwaWQuZ292Lml0L2NlcnRpZmljYXRpb24vcnAiLCJsb2dvX3VyaSI6Imh0dHBzOi8vd3d3LmFnaWQuZ292Lml0L3RoZW1lcy9jdXN0b20vYWdpZC9sb2dvLnN2ZyIsInJlZiI6Imh0dHBzOi8vZG9jcy5pdGFsaWEuaXQvZG9jcy9zcGlkLWNpZS1vaWRjLWRvY3MvaXQvdmVyc2lvbmUtY29ycmVudGUvIn0.AGf5Y4MoJt22rznH4i7Wqpb2EF2LzE6BFEkTzY1dCBMCK-8P_vj4Boz7335pUF45XXr2jx5_waDRgDoS5vOO-wfc0NWb4Zb_T1RCwcryrzV0z3jJICePMPM_1hZnBZjTNQd4EsFNvKmUo_teR2yzAZjguR2Rid30O5PO8kJtGaXDmz-rWaHbmfLhlNGJnqcp9Lo1bhkU_4Cjpn2bdX7RN0JyfHVY5IJXwdxUMENxZd-VtA5QYiw7kPExT53XcJO89ebe_ik4D0dl-vINwYhrIz2RPnqgA1OdbK7jg0vm8Tb3aemRLG7oLntHwqLO-gGYr6evM2_SgqwA0lQ9mB9yhw\"" + //
        "}\n";
    private static final String TRUST_MARKS = "[" + TRUST_MARK + "]";

    private OpenIdFedIdentityProviderConfig providerConfig;
    private String baseUrl = "https://rp.example.it";

    @BeforeEach
    public void setUp() {
        OpenIdFedIdentityProviderConfigMap map = new OpenIdFedIdentityProviderConfigMap();
        map.setClientName("AAC");
        map.setOrganizationName("Organization");
        map.setContacts(List.of("admin@rp", "support@rp"));

        map.setTrustAnchor("https://trust-anchor.org");
        map.setAuthorityHints(Collections.singleton("https://registry.example.it/"));
        map.setTrustMarks(TRUST_MARKS);

        map.setFederationJwks(KEY);
        map.setClientJwks(KEYS);
        map.setSubjectType(SubjectType.PAIRWISE);
        map.setPromptMode(Set.of(PromptMode.CONSENT, PromptMode.LOGIN));
        map.setScope("profile");
        map.setUserInfoJWEAlg(JWEAlgorithm.RSA_OAEP);
        map.setUserInfoJWEEnc(EncryptionMethod.A128CBC_HS256);

        OpenIdFedIdentityProviderConfig config = new OpenIdFedIdentityProviderConfig("test", "test");
        config.setConfigMap(map);

        this.providerConfig = config;
    }

    @Test
    void metadataTest() throws Exception {
        OpenIdFedIdentityProviderConfigMap map = providerConfig.getConfigMap();
        DefaultOpenIdRpMetadataResolver resolver = new DefaultOpenIdRpMetadataResolver();
        EntityStatement statement = resolver.generateEntityStatement(providerConfig, baseUrl);

        //entityId is built correctly
        String entityId = baseUrl + "/auth/openidfed/metadata/" + providerConfig.getProvider();
        assertEquals(entityId, statement.getEntityID().toString());

        //issuer matches
        assertEquals(entityId, statement.getClaimsSet().getIssuer().toString());

        //time interval is correct
        Date now = Date.from(Instant.now());
        assertThat(statement.getClaimsSet().getIssueTime()).isBefore(now);
        assertThat(statement.getClaimsSet().getExpirationTime()).isAfter(now);

        //federation public key is available
        assertThat(statement.getClaimsSet().getJWKSet().getKeyByKeyId("cce6d4e8-e8a4-4ceb-a9ca-9b6336220763"))
            .isNotNull();

        //authority hints
        assertEquals(
            map.getAuthorityHints(),
            statement.getClaimsSet().getAuthorityHints().stream().map(e -> e.toString()).collect(Collectors.toSet())
        );

        //trust marks
        assertThat(statement.getClaimsSet().getTrustMarks()).isNotEmpty();
        //federation metadata
        assertEquals(
            map.getOrganizationName(),
            statement.getClaimsSet().getFederationEntityMetadata().getOrganizationName()
        );
        assertEquals(map.getContacts(), statement.getClaimsSet().getFederationEntityMetadata().getContacts());

        //redirect uri contains login
        assertEquals(
            URI.create(baseUrl + "/auth/openidfed/resolve/" + providerConfig.getProvider()),
            statement.getClaimsSet().getFederationEntityMetadata().getFederationResolveEndpointURI()
        );

        //rp metadata
        assertEquals(entityId, statement.getClaimsSet().getRPInformation().getID().toString());
        assertEquals(map.getClientName(), statement.getClaimsSet().getRPMetadata().getName());
        assertEquals(map.getSubjectType(), statement.getClaimsSet().getRPMetadata().getSubjectType());

        //redirect uri contains login
        assertThat(statement.getClaimsSet().getRPMetadata().getRedirectionURIs())
            .contains(URI.create(baseUrl + "/auth/openidfed/login/" + providerConfig.getProvider()));

        //auth_code flow is declared
        assertThat(statement.getClaimsSet().getRPMetadata().getGrantTypes()).contains(GrantType.AUTHORIZATION_CODE);
        assertThat(statement.getClaimsSet().getRPMetadata().getResponseTypes()).contains(ResponseType.CODE);

        //application type is web
        assertEquals(ApplicationType.WEB, statement.getClaimsSet().getRPMetadata().getApplicationType());

        //registration is automatic
        assertThat(statement.getClaimsSet().getRPMetadata().getClientRegistrationTypes())
            .contains(ClientRegistrationType.AUTOMATIC);

        //scopes are requested
        assertThat(statement.getClaimsSet().getRPMetadata().getScope().toStringList()).contains("openid", "profile");

        //keys
        assertThat(
            statement.getClaimsSet().getRPMetadata().getJWKSet().getKeyByKeyId("cce6d4e8-e8a4-4ceb-a9ca-9b6336220763")
        )
            .isNotNull();

        //algorithms
        assertEquals(
            JWSAlgorithm.RS256.getValue(),
            statement.getClaimsSet().getRPMetadata().getIDTokenJWSAlg().getName()
        );
        assertEquals(
            JWSAlgorithm.RS256.getValue(),
            statement.getClaimsSet().getRPMetadata().getUserInfoJWSAlg().getName()
        );
        assertEquals(
            map.getUserInfoJWEAlg().getValue(),
            statement.getClaimsSet().getRPMetadata().getUserInfoJWEAlg().getName()
        );
        assertEquals(
            map.getUserInfoJWEAlg().getValue(),
            statement.getClaimsSet().getRPMetadata().getUserInfoJWEAlg().getName()
        );
        assertEquals(
            map.getUserInfoJWEEnc().toString(),
            statement.getClaimsSet().getRPMetadata().getUserInfoJWEEnc().getName()
        );
    }

    @Test
    public void authRequestTest() throws Exception {
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository =
            new InMemoryProviderConfigRepository<>();
        registrationRepository.addRegistration(providerConfig);
        OpenIdFedOAuth2AuthorizationRequestResolver resolver = new OpenIdFedOAuth2AuthorizationRequestResolver(
            registrationRepository,
            baseUrl
        );

        //TODO mock an openid provider
        String url = baseUrl + "/authorize/" + providerConfig.getProvider();
    }
}
