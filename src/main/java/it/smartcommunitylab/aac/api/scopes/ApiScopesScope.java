package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ScopeType;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiScopesScope extends ApiScope {

    public static final String SCOPE = "aac.api.scopes";

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
        return "Manage scopes";
    }

    @Override
    public String getDescription() {
        return "Manage resources and scopes";
    }

    @Override
    public Set<String> getAuthorities() {
        return Stream.of(Config.R_ADMIN, Config.R_DEVELOPER).collect(Collectors.toCollection(HashSet::new));
    }
}
