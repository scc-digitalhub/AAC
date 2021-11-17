package it.smartcommunitylab.aac.api.scopes;

import java.util.Collections;
import java.util.Set;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ScopeType;

public class ApiProviderScope extends ApiScope {

    public static final String SCOPE = "aac.api.provider";

    @Override
    public String getScope() {
        return SCOPE;
    }

    @Override
    public ScopeType getType() {
        return ScopeType.USER;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Manage providers";
    }

    @Override
    public String getDescription() {
        return "Manage identity and attribute providers.";
    }

    @Override
    public Set<String> getAuthorities() {
        return Collections.singleton(Config.R_ADMIN);
    }

}
