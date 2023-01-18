package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class AACApiResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = "aac.api";

    public AACApiResource(String realm, String baseUrl) {
        super(realm, baseUrl, RESOURCE_ID);

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
    public AACApiResource(String realm, String baseUrl, AbstractInternalApiScope... scopes) {
        super(realm, baseUrl, RESOURCE_ID);
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

}