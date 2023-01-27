package it.smartcommunitylab.aac.claims.extractors;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.claims.model.ClaimsExtractor;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;

public class WebhookClaimsExtractor implements ClaimsExtractor<AbstractClaim> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Collection<AbstractClaim> extractUserClaims(User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<AbstractClaim> extractClientClaims(ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        // TODO Auto-generated method stub
        return null;
    }

}
