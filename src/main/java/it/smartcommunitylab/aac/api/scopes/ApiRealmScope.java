package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ScopeType;
import java.util.Collections;
import java.util.Set;

public class ApiRealmScope extends ApiScope {

    public static final String SCOPE = "aac.api.realm";

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
        return "Manage realm";
    }

    @Override
    public String getDescription() {
        return "Manage realm settings and customization.";
    }

    @Override
    public Set<String> getAuthorities() {
        return Collections.singleton(Config.R_ADMIN);
    }
}
