package it.smartcommunitylab.aac.openid;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainResolver;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainSet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// @SpringBootTest
// @AutoConfigureMockMvc
// @ActiveProfiles("test")
public class OIDCFedTest {

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
}
