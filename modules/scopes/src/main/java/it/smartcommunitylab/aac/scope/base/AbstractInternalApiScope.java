package it.smartcommunitylab.aac.scope.base;

import it.smartcommunitylab.aac.SystemKeys;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public abstract class AbstractInternalApiScope extends AbstractApiScope {

    protected final String scope;

    // resourceId is providerId for internal api
    private final String resourceId;
    private final String id;

    // internal api are protected via authorities
    // do note that we will restrict authorities to matching realm on approval
    protected Set<String> authorities;
    protected String policy;

    // we could also restrict subject types
    protected String subjectType;

    protected AbstractInternalApiScope(String authority, String realm, String resourceId, String scope) {
        super(authority, resourceId);
        Assert.hasText(realm, "realm can not be null or empty");
        Assert.hasText(resourceId, "resourceId can not be null or empty");
        Assert.hasText(scope, "scope can not be null or empty");

        this.realm = realm;
        this.resourceId = resourceId;
        this.scope = scope;

        // build id according to schema
        this.id = resourceId + SystemKeys.ID_SEPARATOR + scope;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<String> authorities) {
        Assert.notNull(authorities, "authorities can not be null");
        this.authorities = Collections.unmodifiableSet(new TreeSet<>(authorities));
    }

    public void setAuthorities(String... authorities) {
        setAuthorities(Arrays.asList(authorities));
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String type) {
        this.subjectType = type;
    }

    // i18n: use id as key for language files
    @Override
    public String getName() {
        return "scopes." + scope + "." + ".title";
    }

    @Override
    public String getDescription() {
        return "scopes." + scope + "." + ".description";
    }

    // policy
    @Override
    public String getPolicy() {
        if (policy == null) {
            policy = buildPolicy();
        }

        return policy;
    }

    protected String buildPolicy() {
        String policy = null;
        // build a composite policy where required
        List<String> policies = new ArrayList<>();
        if (authorities != null) {
            // build policy for authorities
            StringBuilder sb = new StringBuilder();
            sb.append("authority(");
            sb.append(StringUtils.collectionToCommaDelimitedString(authorities));
            sb.append(")");
            policies.add(sb.toString());
        }

        if (subjectType != null) {
            // build policy for type
            StringBuilder sb = new StringBuilder();
            sb.append("subjectType(");
            sb.append(subjectType);
            sb.append(")");
            policies.add(sb.toString());
        }

        // build manually
        // TODO refactor via (fluent) builder
        if (policies.size() == 1) {
            policy = policies.get(0);
        } else if (policies.size() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("allOf(");
            sb.append(StringUtils.collectionToCommaDelimitedString(policies));
            sb.append(")");
            policy = sb.toString();
        }

        return policy;
    }
}
