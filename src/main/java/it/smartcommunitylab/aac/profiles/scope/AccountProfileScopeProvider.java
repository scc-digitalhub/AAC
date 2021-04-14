package it.smartcommunitylab.aac.profiles.scope;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.profiles.model.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.Scope;

@Component
public class AccountProfileScopeProvider extends ProfileScopeProvider {

    private static final Set<Scope> scopes;

    static {
        scopes = Collections.unmodifiableSet(Collections.singleton(new AccountProfileScope()));
    }
    
    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID + ".account";
    }

    @Override
    public Collection<Scope> getScopes() {
        return scopes;
    }

}
