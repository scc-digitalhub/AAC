package it.smartcommunitylab.aac.api.scopes;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ScopeType;

public class ApiClientAppScope extends ApiScope {

    public static final String SCOPE = "aac.api.clientapp";

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
        return "Manage client apps";
    }

    @Override
    public String getDescription() {
        return "Manage client applications.";
    }

    @Override
    public Set<String> getAuthorities() {
        return Stream.of(Config.R_ADMIN, Config.R_DEVELOPER)
                .collect(Collectors.toCollection(HashSet::new));
    }

}
