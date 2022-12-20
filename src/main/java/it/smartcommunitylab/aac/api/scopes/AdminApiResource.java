package it.smartcommunitylab.aac.api.scopes;

public class AdminApiResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = "aac.admin";

    public AdminApiResource(String realm) {
        super(realm, RESOURCE_ID);

        // statically register admin scopes
        setScopes(new AdminRealmsScope(realm));
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "AAC Admin api";
    }

    @Override
    public String getDescription() {
        return "Access AAC admin api";
    }

}