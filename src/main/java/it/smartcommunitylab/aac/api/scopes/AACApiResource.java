package it.smartcommunitylab.aac.api.scopes;

public class AACApiResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = "aac.api";

    public AACApiResource(String realm) {
        super(realm, RESOURCE_ID);

        // we don't register scopes statically
        // let provider decide which are available
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "AAC Api";
    }

    @Override
    public String getDescription() {
        return "Access AAC api";
    }

}