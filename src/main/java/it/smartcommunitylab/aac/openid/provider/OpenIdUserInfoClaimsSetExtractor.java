package it.smartcommunitylab.aac.openid.provider;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.claims.base.AbstractResourceClaimsSetExtractor;
import it.smartcommunitylab.aac.claims.base.DefaultClaimsParser;
import it.smartcommunitylab.aac.claims.model.ClaimsExtractor;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.openid.scope.OpenIdAddressScope;
import it.smartcommunitylab.aac.openid.scope.OpenIdDefaultScope;
import it.smartcommunitylab.aac.openid.scope.OpenIdEmailScope;
import it.smartcommunitylab.aac.openid.scope.OpenIdPhoneScope;
import it.smartcommunitylab.aac.openid.scope.OpenIdScope;
import it.smartcommunitylab.aac.openid.scope.OpenIdUserInfoResource;
import it.smartcommunitylab.aac.profiles.extractor.OpenIdProfileExtractor;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;

public class OpenIdUserInfoClaimsSetExtractor extends AbstractResourceClaimsSetExtractor<OpenIdUserInfoResource> {

    public OpenIdUserInfoClaimsSetExtractor(OpenIdUserInfoResource resource) {
        super(resource);
        this.claimsExtractor = new OpenIdUserInfoClaimsExtractor(new OpenIdProfileExtractor(),
                new DefaultClaimsParser(resource.getClaims()));
    }

    public static class OpenIdUserInfoClaimsExtractor implements ClaimsExtractor<AbstractClaim> {
        private final OpenIdProfileExtractor profileExtractor;
        private final Converter<MultiValueMap<String, Serializable>, List<AbstractClaim>> claimsParser;

        public OpenIdUserInfoClaimsExtractor(OpenIdProfileExtractor profileExtractor,
                Converter<MultiValueMap<String, Serializable>, List<AbstractClaim>> claimsParser) {
            Assert.notNull(profileExtractor, "openid profile extractor can not be null");
            Assert.notNull(claimsParser, "claims parser can not be null");

            this.profileExtractor = profileExtractor;
            this.claimsParser = claimsParser;

        }

        @Override
        public Collection<AbstractClaim> extractUserClaims(User user, ClientDetails client,
                Collection<String> scopes,
                Map<String, Serializable> extensions) {

            // check if openid scope is present
            if (scopes == null || !scopes.contains(OpenIdScope.SCOPE)) {
                return null;
            }

            try {
                // build full profile via extractor
                OpenIdProfile profile = profileExtractor.extractUserProfile(user);
                if (profile == null) {
                    return null;
                }

                Set<AbstractClaim> claims = new HashSet<>();

                // filter content based on scopes for claims
                // we serialize the profile and then build the claims by parsing
                // TODO use direct extraction without serialization
                if (scopes.contains(OpenIdDefaultScope.SCOPE)) {
                    List<AbstractClaim> c = claimsParser.convert(toMultiMap(profile.toDefaultProfile().toMap()));
                    if (c != null) {
                        claims.addAll(c);
                    }
                }

                if (scopes.contains(OpenIdEmailScope.SCOPE)) {
                    List<AbstractClaim> c = claimsParser.convert(toMultiMap(profile.toEmailProfile().toMap()));
                    if (c != null) {
                        claims.addAll(c);
                    }
                }

                if (scopes.contains(OpenIdPhoneScope.SCOPE)) {
                    List<AbstractClaim> c = claimsParser.convert(toMultiMap(profile.toPhoneProfile().toMap()));
                    if (c != null) {
                        claims.addAll(c);
                    }
                }

                if (scopes.contains(OpenIdAddressScope.SCOPE)) {
                    List<AbstractClaim> c = claimsParser.convert(toMultiMap(profile.toAddressProfile().toMap()));
                    if (c != null) {
                        claims.addAll(c);
                    }
                }

                return claims;
            } catch (InvalidDefinitionException e) {
                return null;
            }

        }

        @Override
        public Collection<AbstractClaim> extractClientClaims(ClientDetails client, Collection<String> scopes,
                Map<String, Serializable> extensions) {
            // not supported
            return null;
        }

        private MultiValueMap<String, Serializable> toMultiMap(@Nullable Map<String, Serializable> source) {
            LinkedMultiValueMap<String, Serializable> map = new LinkedMultiValueMap<>();
            if (source != null) {
                source.forEach((k, v) -> {
                    map.put(k, Collections.singletonList(v));
                });
            }

            return map;
        }

    }

}
