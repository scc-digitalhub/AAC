package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;

public class AdminApiResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = "aac.admin";

    public AdminApiResource(String realm, String baseUrl) {
        super(realm, baseUrl, RESOURCE_ID);

        // statically register admin scopes
        setScopes(new AdminRealmsScope(realm));

        // no claims for now
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "AAC Admin api";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Access AAC admin api";
//    }

}