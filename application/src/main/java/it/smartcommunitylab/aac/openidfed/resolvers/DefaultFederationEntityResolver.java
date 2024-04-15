/**
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.openidfed.resolvers;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.federation.api.EntityListingRequest;
import com.nimbusds.openid.connect.sdk.federation.api.EntityListingResponse;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class DefaultFederationEntityResolver implements FederationEntityResolver {

    private final EntityStatementResolver entityStatementResolver;

    public DefaultFederationEntityResolver() {
        this(new DefaultEntityStatementResolver());
    }

    public DefaultFederationEntityResolver(EntityStatementResolver entityStatementResolver) {
        Assert.notNull(entityStatementResolver, "entity statement resolver is required");
        this.entityStatementResolver = entityStatementResolver;
    }

    @Override
    public FederationEntityMetadata resolveFederationEntityMetadata(String trustAnchor, String entityId)
        throws ResolveException {
        EntityStatement statement = entityStatementResolver.resolveEntityStatement(trustAnchor, entityId);
        //fallback to direct fetch for trustAnchor because we may fail in building a valid trust chain
        if (statement == null && trustAnchor.equals(entityId)) {
            statement = entityStatementResolver.fetchEntityStatement(entityId);
        }

        if (
            statement == null ||
            statement.getClaimsSet() == null ||
            statement.getClaimsSet().getFederationEntityMetadata() == null
        ) {
            throw new ResolveException("Invalid federation metadata from " + trustAnchor);
        }

        return statement.getClaimsSet().getFederationEntityMetadata();
    }

    @Override
    public List<EntityID> listFederationEntities(String trustAnchor, String entityId, @Nullable EntityType type)
        throws ResolveException {
        //resolve metadata to extract listing endpoint
        FederationEntityMetadata metadata = resolveFederationEntityMetadata(trustAnchor, entityId);

        URI listEndpoint = metadata.getFederationListEndpointURI();
        if (listEndpoint == null) {
            throw new ResolveException("Missing list endpoint for " + trustAnchor);
        }

        //make list request and collect results
        EntityListingRequest request = new EntityListingRequest(listEndpoint);
        if (type != null) {
            request = new EntityListingRequest(listEndpoint, type);
        }

        HTTPRequest httpRequest = request.toHTTPRequest();
        HTTPResponse httpResponse;
        try {
            httpResponse = httpRequest.send();
        } catch (IOException e) {
            throw new ResolveException(
                "Couldn't retrieve entity configuration for " + httpRequest.getURL() + ": " + e.getMessage(),
                e
            );
        }

        try {
            EntityListingResponse listing = EntityListingResponse.parse(httpResponse);
            if (!listing.indicatesSuccess()) {
                throw new ResolveException("Listing entities from " + httpRequest.getURL() + "failed");
            }

            return listing.toSuccessResponse().getEntityListing();
        } catch (ParseException e) {
            throw new ResolveException(
                "Couldn't parse listing response for " + httpRequest.getURL() + ": " + e.getMessage(),
                e
            );
        }
    }
}
