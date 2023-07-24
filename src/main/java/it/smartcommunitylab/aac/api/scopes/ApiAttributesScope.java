package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ScopeType;
import java.util.Collections;
import java.util.Set;

public class ApiAttributesScope extends ApiScope {

    public static final String SCOPE = "aac.api.attributes";

    @Override
    public String getScope() {
        return SCOPE;
    }

    @Override
    public ScopeType getType() {
        return ScopeType.GENERIC;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Manage attribute sets";
    }

    @Override
    public String getDescription() {
        return "Manage custom attribute sets definitions";
    }

    @Override
    public Set<String> getAuthorities() {
        return Collections.singleton(Config.R_ADMIN);
    }
}
