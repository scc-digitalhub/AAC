package it.smartcommunitylab.aac.openid;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainResolver;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainSet;
import it.smartcommunitylab.aac.oauth.model.SubjectType;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openidfed.service.OpenIdFedMetadataResolver;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// @SpringBootTest
// @AutoConfigureMockMvc
// @ActiveProfiles("test")
public class OpenIdFedTest {

    private static final String KEYS =
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

    @Test
    public void libTest() throws Exception {
        // The configured federation trust anchor URL
        EntityID trustAnchor = new EntityID("http://trust-anchor.org:8000");

        // The entity ID (URL) of the OpenID provider to resolve
        EntityID openIDProviderEntity = new EntityID("http://cie-provider.org:8002/oidc/op");

        // Find out if there is a valid trust chain leading from the OpenID provider
        // up to the configured trust anchor
        TrustChainResolver resolver = new TrustChainResolver(trustAnchor);

        TrustChainSet resolvedChains;
        try {
            resolvedChains = resolver.resolveTrustChains(openIDProviderEntity);
        } catch (ResolveException e) {
            // Couldn't resolve a valid trust chain
            System.err.println(e.getMessage());
            return;
        }

        // The process can theoretically resolve multiple chains if multiple achors
        // are configured, choose the shortest
        TrustChain chain = resolvedChains.getShortest();

        // Get the policy for registering a relying party with the OpenID provider
        MetadataPolicy metadataPolicy = chain.resolveCombinedMetadataPolicy(EntityType.OPENID_RELYING_PARTY);
        System.out.println(metadataPolicy.toJSONObject());
    }

    @Test
    public void metadataTest() throws Exception {
        OpenIdFedIdentityProviderConfigMap map = new OpenIdFedIdentityProviderConfigMap();
        map.setClientId("https://rp.example.it/");
        map.setClientName("Name of an example organization");
        map.setFederationJwks(KEYS);

        map.setClientJwks(KEYS);
        map.setSubjectType(SubjectType.PAIRWISE);
        map.setAuthorityHints(Collections.singleton("https://registry.agid.gov.it/"));

        map.setAuthorizationUri("authUrl");
        map.setTokenUri("tokenUrl");
        map.setUserInfoUri("userInfoUrl");

        OpenIdFedIdentityProviderConfig config = new OpenIdFedIdentityProviderConfig("test", "test");
        config.setConfigMap(map);

        OpenIdFedMetadataResolver resolver = new OpenIdFedMetadataResolver();
        EntityStatement statement = resolver.generate(config);
        String meta = statement.getSignedStatement().serialize();
        System.out.println("statement " + meta);
    }
}
