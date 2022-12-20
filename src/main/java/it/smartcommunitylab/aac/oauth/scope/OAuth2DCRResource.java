package it.smartcommunitylab.aac.oauth.scope;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.AbstractInternalApiResource;

public class OAuth2DCRResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = "oauth2.dcr";

    public OAuth2DCRResource(String realm) {
        super(SystemKeys.AUTHORITY_OAUTH2, realm, RESOURCE_ID);

        // statically register scopes
        setScopes(new OAuth2DCRScope(realm));
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "OAuth2 Dynamic Client Registration";
    }

    @Override
    public String getDescription() {
        return "OAuth2 Dynamic Client Registration";
    }
}
