package it.smartcommunitylab.aac.profiles.scope;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.profiles.model.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.Scope;

@Component
public class OpenIdScopeProvider extends ProfileScopeProvider {

    private static final Set<Scope> scopes;

    static {
        Set<Scope> s = new HashSet<>();
        s.add(new OpenIdProfileScope());
        s.add(new OpenIdEmailScope());
        s.add(new OpenIdDefaultScope());
        s.add(new OpenIdAddressScope());
        s.add(new OpenIdPhoneScope());

        scopes = Collections.unmodifiableSet(s);
    }

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID + ".openid";
    }

    @Override
    public Collection<Scope> getScopes() {
        return scopes;
    }

}
