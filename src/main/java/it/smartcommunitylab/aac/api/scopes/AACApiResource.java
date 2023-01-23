package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.api.AACApiResourceAuthority;
import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class AACApiResource extends
        AbstractInternalApiResource<it.smartcommunitylab.aac.api.scopes.AACApiResource.AbstractAACApiScope, AbstractClaimDefinition> {

    public static final String RESOURCE_ID = "aac.api";
    public static final String AUTHORITY = AACApiResourceAuthority.AUTHORITY;

    public AACApiResource(String realm, String baseUrl) {
        super(AUTHORITY, realm, baseUrl, RESOURCE_ID);

        // set scopes
        setScopes(
                new ApiAttributesScope(realm),
                new ApiAuditScope(realm),
                new ApiClientAppScope(realm),
                new ApiGroupsScope(realm),
                new ApiProviderScope(realm),
                new ApiRealmScope(realm),
                new ApiRolesScope(realm),
                new ApiScopesScope(realm),
                new ApiServicesScope(realm),
                new ApiUsersScope(realm));

        // no claims for now
    }

    // we don't register scopes statically
    // let provider decide which are available
    public AACApiResource(String realm, String baseUrl, AbstractAACApiScope... scopes) {
        super(AUTHORITY, realm, baseUrl, RESOURCE_ID);
        setScopes(scopes);
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "AAC Api";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Access AAC api";
//    }
    public static class AbstractAACApiScope extends AbstractInternalApiScope {

        public AbstractAACApiScope(String realm, String scope) {
            super(AUTHORITY, realm, RESOURCE_ID, scope);
        }

    }
}