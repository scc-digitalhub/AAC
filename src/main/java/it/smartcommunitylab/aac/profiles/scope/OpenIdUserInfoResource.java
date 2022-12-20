package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.AbstractInternalApiResource;

public class OpenIdUserInfoResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = "openid.userinfo";

    public OpenIdUserInfoResource(String realm) {
        super(SystemKeys.AUTHORITY_OIDC, realm, RESOURCE_ID);

        // statically register scopes
        // TODO evaluate making configurable
        setScopes(
                new OpenIdDefaultScope(realm),
                new OpenIdEmailScope(realm),
                new OpenIdAddressScope(realm),
                new OpenIdPhoneScope(realm));
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "OpenId user info";
    }

    @Override
    public String getDescription() {
        return "Access user openid profile: basic, email, address, phone";
    }
}
