package it.smartcommunitylab.aac.profiles.scope;

import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.approver.SubjectTypeScopeApprover;
import it.smartcommunitylab.aac.scope.base.AbstractApiScopeProvider;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

/*
 * A simple scope provider which return profile scopes
 * 
 */

public class ProfileScopeProvider extends AbstractApiScopeProvider<AbstractInternalApiScope> {

    public ProfileScopeProvider(ProfileApiResource resource) {
        super(SystemKeys.AUTHORITY_INTERNAL, resource.getProvider(), resource.getScopes());
        Assert.notNull(resource, "resource can not be null");
    }

    @Override
    protected SubjectTypeScopeApprover<AbstractInternalApiScope> buildScopeApprover(AbstractInternalApiScope s) {
        // build approver based on type
        SubjectTypeScopeApprover<AbstractInternalApiScope> sa = new SubjectTypeScopeApprover<>(s);
        sa.setSubjectType(s.getSubjectType());

        return sa;
    }

//
//    private final ProfileApiResource resource = new ProfileApiResource();
//    public static final Set<Scope> scopes;
//    private static final Map<String, WhitelistScopeApprover> approvers;
//
//    static {
//        Set<Scope> s = new HashSet<>();
//        s.add(new BasicProfileScope());
//        s.add(new AccountProfileScope());
//
//        scopes = Collections.unmodifiableSet(s);
//
//        Map<String, WhitelistScopeApprover> a = new HashMap<>();
//        for (Scope sc : scopes) {
//            a.put(sc.getScope(), new WhitelistScopeApprover(null, sc.getResourceId(), sc.getScope()));
//        }
//
//        approvers = a;
//    }
//
//    private final AttributeService attributeService;
//
//    // loading cache for set profile approvers
//    private final LoadingCache<String, WhitelistScopeApprover> setApprovers = CacheBuilder.newBuilder()
//            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
//            .maximumSize(100)
//            .build(new CacheLoader<String, WhitelistScopeApprover>() {
//                @Override
//                public WhitelistScopeApprover load(final String scope) throws Exception {
//                    return new WhitelistScopeApprover(null, resource.getResourceId(), scope);
//                }
//            });
//
//    public ProfileScopeProvider(AttributeService attributeService) {
//        Assert.notNull(attributeService, "attribute service is required");
//        this.attributeService = attributeService;
//    }
//
//    @Override
//    public String getResourceId() {
//        return ProfileClaimsSet.RESOURCE_ID;
//    }
//
//    @Override
//    public ApiResource getResource() {
//        resource.setScopes(getScopes());
//        return resource;
//    }
//
//    @Override
//    public Collection<Scope> getScopes() {
//        Set<Scope> res = new HashSet<>();
//        res.addAll(scopes);
//
//        attributeService.listAttributeSets().stream().forEach(a -> {
//            res.add(new CustomProfileScope(a.getIdentifier()));
//        });
//
//        return res;
//    }
//
//    @Override
//    public ScopeApprover getApprover(String scope) {
//        if (approvers.containsKey(scope)) {
//            return approvers.get(scope);
//        }
//
//        // check if scope is a set id
//        String id = extractId(scope);
//        AttributeSet set = attributeService.findAttributeSet(id);
//        if (set != null) {
//            try {
//                return setApprovers.get(scope);
//            } catch (ExecutionException e) {
//                return null;
//            }
//        }
//
//        return null;
//    }
//
//    private String extractId(String scope) {
//        if (scope.startsWith("profile.") && scope.endsWith(".me")) {
//            return scope.substring(8, scope.length() - 3);
//        }
//        return scope;
//    }

}
