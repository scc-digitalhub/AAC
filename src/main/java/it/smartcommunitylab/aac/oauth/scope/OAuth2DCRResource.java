package it.smartcommunitylab.aac.oauth.scope;

import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.oauth.OAuth2DCRResourceAuthority;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;

public class OAuth2DCRResource extends
        AbstractInternalApiResource<OAuth2DCRScope, AbstractClaimDefinition> {

    public static final String RESOURCE_ID = "oauth2.dcr";
    public static final String AUTHORITY = OAuth2DCRResourceAuthority.AUTHORITY;

    public OAuth2DCRResource(String realm, String baseUrl) {
        super(AUTHORITY, realm, baseUrl, RESOURCE_ID);

        // statically register scopes
        setScopes(new OAuth2DCRScope(realm));
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "OAuth2 Dynamic Client Registration";
//    }
//
//    @Override
//    public String getDescription() {
//        return "OAuth2 Dynamic Client Registration";
//    }
}
