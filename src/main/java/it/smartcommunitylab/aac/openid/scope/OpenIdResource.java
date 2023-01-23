package it.smartcommunitylab.aac.openid.scope;

import it.smartcommunitylab.aac.claims.base.AbstractClaimDefinition;
import it.smartcommunitylab.aac.openid.OpenIdResourceAuthority;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class OpenIdResource extends
        AbstractInternalApiResource<it.smartcommunitylab.aac.openid.scope.OpenIdResource.AbstractOpenIdScope, AbstractClaimDefinition> {

    public static final String RESOURCE_ID = "openid.oidc";
    public static final String AUTHORITY = OpenIdResourceAuthority.AUTHORITY;

    public OpenIdResource(String realm, String baseUrl) {
        super(AUTHORITY, realm, baseUrl, RESOURCE_ID);

        // statically register scopes
        setScopes(
                new OpenIdScope(realm),
                new OfflineAccessScope(realm));
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "OpenId Connect";
//    }
//
//    @Override
//    public String getDescription() {
//        return "OpenId Connect core";
//    }

    public static class AbstractOpenIdScope extends AbstractInternalApiScope {

        public AbstractOpenIdScope(String realm, String scope) {
            super(AUTHORITY, realm, RESOURCE_ID, scope);
        }

    }
}
