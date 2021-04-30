package it.smartcommunitylab.aac.roles;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import it.smartcommunitylab.aac.scope.Resource;
import it.smartcommunitylab.aac.scope.Scope;

public class RolesResource extends Resource {

    public static final String RESOURCE_ID = "aac.roles";

    private static final Set<Scope> scopes;

    static {
        scopes = Collections.unmodifiableSet(Collections.singleton(new RolesScope()));
    }

    @Override
    public String getResourceId() {
        return RESOURCE_ID;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "User roles";
    }

    @Override
    public String getDescription() {
        return "Access user roles and groups";
    }

    @Override
    public Collection<Scope> getScopes() {
        return scopes;
    }
}