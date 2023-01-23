package it.smartcommunitylab.aac.openid.provider;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.claims.base.AbstractResourceClaimsExtractor;
import it.smartcommunitylab.aac.claims.model.BooleanClaim;
import it.smartcommunitylab.aac.claims.model.DateClaim;
import it.smartcommunitylab.aac.claims.model.StringClaim;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.AttributeType;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.openid.scope.OpenIdAddressScope;
import it.smartcommunitylab.aac.openid.scope.OpenIdDefaultScope;
import it.smartcommunitylab.aac.openid.scope.OpenIdEmailScope;
import it.smartcommunitylab.aac.openid.scope.OpenIdPhoneScope;
import it.smartcommunitylab.aac.openid.scope.OpenIdScope;
import it.smartcommunitylab.aac.openid.scope.OpenIdUserInfoResource;
import it.smartcommunitylab.aac.profiles.extractor.OpenIdProfileExtractor;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;

public class OpenIdUserInfoClaimsExtractor extends AbstractResourceClaimsExtractor<OpenIdUserInfoResource> {
    private final OpenIdProfileExtractor extractor;

    public OpenIdUserInfoClaimsExtractor(OpenIdUserInfoResource resource) {
        super(resource);
        this.extractor = new OpenIdProfileExtractor();
    }

    @Override
    protected Collection<AbstractClaim> extractUserClaims(User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {

        // check if openid scope is present
        if (scopes == null || !scopes.contains(OpenIdScope.SCOPE)) {
            return null;
        }

        try {
            // build full profile via extractor
            OpenIdProfile profile = extractor.extractUserProfile(user);
            Set<AbstractClaim> claims = new HashSet<>();

            // filter content based on scopes for claims
            // we serialize the profile and then build the claims by parsing
            // TODO use direct extraction without serialization
            if (scopes.contains(OpenIdDefaultScope.SCOPE)) {
                claims.addAll(buildClaims(profile.toDefaultProfile().toMap()));
            }

            if (scopes.contains(OpenIdEmailScope.SCOPE)) {
                claims.addAll(buildClaims(profile.toEmailProfile().toMap()));
            }

            if (scopes.contains(OpenIdPhoneScope.SCOPE)) {
                claims.addAll(buildClaims(profile.toPhoneProfile().toMap()));
            }

            if (scopes.contains(OpenIdAddressScope.SCOPE)) {
                claims.addAll(buildClaims(profile.toAddressProfile().toMap()));
            }

            return claims;
        } catch (InvalidDefinitionException e) {
            return null;
        }

    }

    private List<AbstractClaim> buildClaims(HashMap<String, Serializable> props) {
        return props.entrySet().stream()
                .filter(e -> definitions.containsKey(e.getKey()))
                .map(e -> buildClaim(definitions.get(e.getKey()), e.getValue()))
                .filter(c -> c != null)
                .collect(Collectors.toList());
    }

    private AbstractClaim buildClaim(AbstractClaimDefinition definition, Serializable value) {
        try {
            if (definition.getAttributeType() == AttributeType.BOOLEAN) {
                return new BooleanClaim(definition.getKey(), BooleanAttribute.parseValue(value));
            }

            if (definition.getAttributeType() == AttributeType.DATE) {
                return new DateClaim(definition.getKey(), DateAttribute.parseValue(value));
            }

            if (definition.getAttributeType() == AttributeType.STRING) {
                return new StringClaim(definition.getKey(), StringAttribute.parseValue(value));
            }

            return null;
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    protected Collection<AbstractClaim> extractClientClaims(ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        // not supported
        return null;
    }

}
