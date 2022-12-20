package it.smartcommunitylab.aac.openid.scope;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.AbstractInternalApiResource;

public class OpenIdResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = "openid.oidc";

    public OpenIdResource(String realm) {
        super(SystemKeys.AUTHORITY_OIDC, realm, RESOURCE_ID);

        // statically register scopes
        setScopes(
                new OpenIdScope(realm),
                new OfflineAccessScope(realm));
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "OpenId Connect";
    }

    @Override
    public String getDescription() {
        return "OpenId Connect core";
    }
}
