package it.smartcommunitylab.aac.claims;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.claims.model.SerializableClaim;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.core.service.UserTranslatorService;
import it.smartcommunitylab.aac.model.AttributeType;
import it.smartcommunitylab.aac.model.User;

public class DefaultClaimsService implements ClaimsService, InitializingBean {

    public static final String CLAIM_MAPPING_FUNCTION = "claimMapping";

    // claims that should not be overwritten
    private static final Set<String> REGISTERED_CLAIM_NAMES = JWTClaimsSet.getRegisteredNames();
    private static final Set<String> STANDARD_CLAIM_NAMES = IDTokenClaimsSet.getStandardClaimNames();
    private static final Set<String> SYSTEM_CLAIM_NAMES;

    static {
        Set<String> n = new HashSet<>();

        n.add("scope");
        n.add("token_type");
        n.add("client_id");
        n.add("active");
        n.add("roles");
        n.add("groups");
        n.add("username");
        n.add("preferred_username");
        n.add("user_name");
        n.add("space");
        n.add("accounts");
        n.add("realm");
        n.add("session");

        SYSTEM_CLAIM_NAMES = Collections.unmodifiableSet(n);
    }

    // TODO add spaceRole service
    private final ExtractorsRegistry extractorsRegistry;
    private ScriptExecutionService executionService;
    private UserService userService;

    // object mapper
    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<HashMap<String, String>> stringMapTypeRef = new TypeReference<HashMap<String, String>>() {
    };
    private final TypeReference<HashMap<String, Serializable>> serMapTypeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    public DefaultClaimsService(ExtractorsRegistry extractorsRegistry) {
        Assert.notNull(extractorsRegistry, "extractors registry is mandatory");
        this.extractorsRegistry = extractorsRegistry;
        mapper.setSerializationInclusion(Include.NON_EMPTY);

    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(executionService, "an execution service is required");
        Assert.notNull(userService, "a user  service is required");
    }

    /*
     * build complete claim mapping according to scopes, resourceIds and custom
     * mapping configured in client hook functions
     * 
     * note that we don't validate scopes against client etc, this should be checked
     * elsewhere
     */
    @Override
    public Map<String, Serializable> getUserClaims(UserDetails userDetails, String realm, ClientDetails client,
            Collection<String> scopes,
            Collection<String> resourceIds, Map<String, Serializable> extensions)
            throws NoSuchResourceException, InvalidDefinitionException, SystemException {

        Map<String, Serializable> claims = new HashMap<>();

        // reset null lists, we support a configuration where we get only clientMapping
        if (scopes == null) {
            scopes = Collections.emptyList();
        }
        if (resourceIds == null) {
            resourceIds = Collections.emptyList();
        }

        // base information, could be overwritten by converters
        claims.put("sub", userDetails.getSubjectId());

        if (scopes.contains(Config.SCOPE_PROFILE)) {
            // realm should stay behind scope "profile", if client doesn't match the realm
            // it should ask for this info and be approved
            claims.put("realm", userDetails.getRealm());

            // TODO evaluate an additional ID to mark this specific userDetails (ie subject
            // + the identities loaded) so clients will be able to distinguish identities
            // sets for the same subject. Could be as simple as an hash of all userIds
            // sorted alphabetically
        }

        // build scopeClaims
        for (String scope : scopes) {
            Collection<ScopeClaimsExtractor> exts = extractorsRegistry.getScopeExtractors(scope);
            for (ScopeClaimsExtractor ce : exts) {
                // each extractor can respond, we keep only userClaims
                User user = userService.getUser(userDetails, ce.getRealm());

                // filter attribute sets according to scopes
                if (!scopes.contains(Config.SCOPE_FULL_PROFILE)) {
                    user.setAttributes(narrowUserAttributes(user.getAttributes(), scopes));
                }

                if (!scopes.contains(Config.SCOPE_ROLE)) {
                    user.setAuthorities(null);
                    user.setRoles(null);
                }

                ClaimsSet cs = ce.extractUserClaims(scope, user, client, scopes, extensions);
                if (cs != null && cs.isUser()) {
                    claims.putAll(extractClaims(cs));
                }
            }

        }

        // build resourceClaims
        // serve a service with no scopes but included as audience
        for (String resourceId : resourceIds) {
            Collection<ResourceClaimsExtractor> exts = extractorsRegistry.getResourceExtractors(resourceId);
            for (ResourceClaimsExtractor ce : exts) {
                // each extractor can respond, we keep only userClaims
                User user = userService.getUser(userDetails, ce.getRealm());

                // filter attribute sets according to scopes
                if (!scopes.contains(Config.SCOPE_FULL_PROFILE)) {
                    user.setAttributes(narrowUserAttributes(user.getAttributes(), scopes));
                }

                if (!scopes.contains(Config.SCOPE_ROLE)) {
                    user.setAuthorities(null);
                    user.setRoles(null);
                }

                ClaimsSet cs = ce.extractUserClaims(resourceId, user, client, scopes, extensions);
                if (cs != null && cs.isUser()) {
                    claims.putAll(extractClaims(cs));
                }
            }

        }

//
//        // process basic scopes from userDetails
//        if (scopes.contains(Config.SCOPE_OPENID)) {
//            Map<String, Serializable> openIdClaims = this.getUserClaimsFromOpenIdProfile(user, scopes);
//            claims.putAll(openIdClaims);
//        }
//
//        if (scopes.contains(Config.SCOPE_BASIC_PROFILE)) {
//            Map<String, Serializable> profileClaims = this.getUserClaimsFromBasicProfile(user);
//            claims.putAll(profileClaims);
//        }
//
//        if (scopes.contains(Config.SCOPE_ACCOUNT_PROFILE)) {
//            ArrayList<Serializable> accountClaims = this.getUserClaimsFromAccountProfile(user);
//            claims.put("accounts", accountClaims);
//        }
//
//        // realm role claims
//        if (scopes.contains(Config.SCOPE_ROLE)) {
//            // TODO read realmRoles from service (need impl)
//        }
//
//        // space roles
//        // TODO
//
//        // group claims
//        // TODO, we need to define groups
//
//        // services can add claims
//        Map<String, Serializable> servicesClaims = new HashMap<>();
//        // TODO
//        Set<String> serviceIds = new HashSet<>();
//        serviceIds.addAll(resourceIds);
//        // resolve scopes to services to integrate list
//        // TODO
//        for (String scope : scopes) {
//            // TODO resolve if matches a service scope, fetch serviceId
//        }
//
//        for (String serviceId : serviceIds) {
//            // narrow down userDetails + clientDetails to match service realm
//            // TODO
//            // call service and let it provide additional data
//            // TODO
//            HashMap<String, Serializable> serviceClaims = null;
//            // enforce prefix via service namespace
//            // TODO
//            String namespace = serviceId;
//            servicesClaims.put(namespace, serviceClaims);
//        }
//
//        // integrate, no clash thanks to namespacing
//        claims.putAll(servicesClaims);

        // freeze claims by keeping keys, these won't be modifiable
        Set<String> reservedKeys = Collections.unmodifiableSet(claims.keySet());

        // custom mapping
        String customMappingFunction = null;
        if (client.getHookFunctions() != null) {
            customMappingFunction = client.getHookFunctions().get(CLAIM_MAPPING_FUNCTION);
        }

        if (StringUtils.hasText(customMappingFunction)) {
            // execute custom mapping function via executor
            Map<String, Serializable> customClaims = this.executeClaimMapping(customMappingFunction, claims);
            if (customClaims != null) {
                // add/replace only non-protected claims
                Map<String, Serializable> allowedClaims = customClaims.entrySet().stream()
                        .filter(e -> (!reservedKeys.contains(e.getKey()) &&
                                !REGISTERED_CLAIM_NAMES.contains(e.getKey()) &&
                                !STANDARD_CLAIM_NAMES.contains(e.getKey()) &&
                                !SYSTEM_CLAIM_NAMES.contains(e.getKey())))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
                claims.putAll(allowedClaims);
            }
        }

        return claims;
    }

    @Override
    public Map<String, Serializable> getClientClaims(ClientDetails client, Collection<String> scopes,
            Collection<String> resourceIds, Map<String, Serializable> extensions)
            throws NoSuchResourceException, InvalidDefinitionException, SystemException {

        Map<String, Serializable> claims = new HashMap<>();

        // reset null lists, we support a configuration where we get only clientMapping
        if (scopes == null) {
            scopes = Collections.emptyList();
        }
        if (resourceIds == null) {
            resourceIds = Collections.emptyList();
        }

        // base information, could be overwritten by converters
        String clientId = client.getClientId();
        claims.put("sub", clientId);
        claims.put("cid", clientId);

        // build scopeClaims
        for (String scope : scopes) {
            Collection<ScopeClaimsExtractor> exts = extractorsRegistry.getScopeExtractors(scope);
            for (ScopeClaimsExtractor ce : exts) {
                // each extractor can respond, we keep only userClaims
                ClaimsSet cs = ce.extractClientClaims(scope, client, scopes, extensions);
                if (cs != null && cs.isClient()) {
                    claims.putAll(extractClaims(cs));
                }
            }

        }

        // build resourceClaims
        // serve a service with no scopes but included as audience
        for (String resourceId : resourceIds) {
            Collection<ResourceClaimsExtractor> exts = extractorsRegistry.getResourceExtractors(resourceId);
            for (ResourceClaimsExtractor ce : exts) {
                // each extractor can respond, we keep only userClaims
                ClaimsSet cs = ce.extractClientClaims(resourceId, client, scopes, extensions);
                if (cs != null && cs.isClient()) {
                    claims.putAll(extractClaims(cs));
                }
            }

        }

        // freeze claims by keeping keys, these won't be modifiable
        Set<String> reservedKeys = Collections.unmodifiableSet(claims.keySet());

        // custom mapping
        String customMappingFunction = null;
        if (client.getHookFunctions() != null) {
            customMappingFunction = client.getHookFunctions().get(CLAIM_MAPPING_FUNCTION);
        }

        if (StringUtils.hasText(customMappingFunction)) {
            // execute custom mapping function via executor
            Map<String, Serializable> customClaims = this.executeClaimMapping(customMappingFunction, claims);
            // add/replace only non-protected claims
            Map<String, Serializable> allowedClaims = customClaims.entrySet().stream()
                    .filter(e -> (!reservedKeys.contains(e.getKey()) &&
                            !REGISTERED_CLAIM_NAMES.contains(e.getKey()) &&
                            !STANDARD_CLAIM_NAMES.contains(e.getKey()) &&
                            !SYSTEM_CLAIM_NAMES.contains(e.getKey())))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            claims.putAll(allowedClaims);
        }

        return claims;
    }

    public Map<String, Serializable> extractClaims(ClaimsSet set) {
        if (set != null) {

            // check for namespace, if present we avoid collisions with reserved
            // we don't care about namespace collisions here
            if (StringUtils.hasText(set.getNamespace())) {

                return Collections.singletonMap(set.getNamespace(), claimsToMap(set.getClaims()));
            } else if (set.getResourceId().startsWith("aac.")) {
                // we let internal map to tld, no checks
                return claimsToMap(set.getClaims());
            } else {
                // we let map to tld only for not-reserved claims
                HashMap<String, Serializable> map = claimsToMap(set.getClaims());

                return map.entrySet().stream().filter(
                        e -> (!REGISTERED_CLAIM_NAMES.contains(e.getKey()) &&
                                !STANDARD_CLAIM_NAMES.contains(e.getKey()) &&
                                !SYSTEM_CLAIM_NAMES.contains(e.getKey())))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

            }

        }

        return null;
    }

    // custom converter from claims list to values map
    // TODO rework
    public HashMap<String, Serializable> claimsToMap(Collection<Claim> claims) {
        // we build a tree where
        // namespace -> collection of claims via key
        // with multiple claims with the same key grouped under a collection
        // do note that claim namespace is DIFFERENT from claimsSet namespace.
        HashMap<String, List<Claim>> map = new HashMap<>();
        map.put("_", new ArrayList<>());
        // put under namespaces
        for (Claim c : claims) {
            String namespace = c.getNamespace();
            if (!StringUtils.hasText(namespace)) {
                namespace = "_";
            }
            if (!map.containsKey(namespace)) {
                map.put(c.getNamespace(), new ArrayList<>());
            }
            map.get(namespace).add(c);
        }

        // flatten as result
        HashMap<String, Serializable> result = new HashMap<>();
        for (String namespace : map.keySet()) {
            List<Claim> cs = map.get(namespace);
            // map to values as collections
            Map<String, ArrayList<Serializable>> contents = new HashMap<>();
            for (Claim c : cs) {
                // handle raw types here
                if (!AttributeType.OBJECT.equals(c.getType())) {
                    addContent(contents, c.getKey(), getClaimValue(c.getType(), c.getValue()));
                } else {
                    // object type can be without key
                    if (c.getKey() == null && c instanceof SerializableClaim) {
                        // we unwrap the object and merge
                        Map<String, Serializable> unwrapped = unwrap((SerializableClaim) c);
                        for (String k : unwrapped.keySet()) {
                            addContent(contents, k, unwrapped.get(k));
                        }
                    } else {
                        // add as exported
                        addContent(contents, c.getKey(), getClaimValue(c.getType(), c.getValue()));
                    }
                }
            }

            // flatten single value collections + TLD
            HashMap<String, Serializable> content = new HashMap<>();
            for (String key : contents.keySet()) {
                ArrayList<Serializable> list = contents.get(key);

                if (list.size() > 1) {
                    content.put(key, list);
                } else {
                    content.put(key, list.get(0));
                }
            }

            if ("_".equals(namespace)) {
                // append to root
                result.putAll(content);
            } else {
                // append as namespace
                result.put(namespace, content);
            }

        }

        return result;
    }

    // export claim value
    // TODO implement export per-type
    private Serializable getClaimValue(AttributeType type, Serializable value) {
        return value;
    }

    private Map<String, Serializable> unwrap(SerializableClaim claim) {
        if (AttributeType.OBJECT.equals(claim.getType())) {
            return mapper.convertValue(claim.getValue(), serMapTypeRef);
        }

        return null;
    }

    private void addContent(Map<String, ArrayList<Serializable>> contents, String key, Serializable value) {

        if (!contents.containsKey(key)) {
            contents.put(key, new ArrayList<>());
        }

        // add
        contents.get(key).add(value);
    }

    public List<UserAttributes> narrowUserAttributes(Collection<UserAttributes> attributes,
            Collection<String> scopes) {
        return attributes.stream()
                .filter(at -> scopes.contains(at.getIdentifier()))
                .collect(Collectors.toList());
    }

//    public Map<String, Serializable> getUserClaimsFromBasicProfile(UserDetails user) {
//        Map<String, Serializable> claims = new HashMap<>();
//
//        // fetch identities
//        Collection<UserIdentity> identities = user.getIdentities();
//
//        if (!identities.isEmpty()) {
//            // TODO decide how to merge identities into a single profile
//            // for now get first identity, should be last logged in
//            BasicProfile profile = identities.iterator().next().toBasicProfile();
//
//            // convert via jackson mapper
//            claims.putAll(mapper.convertValue(profile, stringMapTypeRef));
//
//        }
//
//        return claims;
//    }
//
//    public Map<String, Serializable> getUserClaimsFromOpenIdProfile(UserDetails user, Collection<String> scopes) {
//        Map<String, Serializable> claims = new HashMap<>();
//
//        OpenIdProfile profile = null;
//
//        // fetch identities
//        Collection<UserIdentity> identities = user.getIdentities();
//
//        if (!identities.isEmpty()) {
//            // TODO decide how to merge identities into a single profile
//            // for now get first identity, should be last logged in
//            profile = identities.iterator().next().toOpenIdProfile();
//
//        }
//
//        // build result according to scopes, via narrow down
//        if (scopes.contains(Config.SCOPE_PROFILE)) {
//            // narrow down and convert via jackson mapper
//            claims.putAll(mapper.convertValue(profile.toDefaultProfile(), stringMapTypeRef));
//        }
//        if (scopes.contains(Config.SCOPE_EMAIL)) {
//            // narrow down and convert via jackson mapper
//            claims.putAll(mapper.convertValue(profile.toEmailProfile(), stringMapTypeRef));
//        }
//        if (scopes.contains(Config.SCOPE_ADDRESS)) {
//            // narrow down and convert via jackson mapper
//            claims.putAll(mapper.convertValue(profile.toAddressProfile(), stringMapTypeRef));
//        }
//        if (scopes.contains(Config.SCOPE_PHONE)) {
//            // narrow down and convert via jackson mapper
//            claims.putAll(mapper.convertValue(profile.toPhoneProfile(), stringMapTypeRef));
//        }
//
//        return claims;
//    }
//
//    public ArrayList<Serializable> getUserClaimsFromAccountProfile(UserDetails user) {
//        ArrayList<Serializable> claims = new ArrayList<>();
//
//        // fetch identities
//        Collection<UserIdentity> identities = user.getIdentities();
//
//        for (UserIdentity identity : identities) {
//            // get account and translate
//            AccountProfile profile = identity.getAccount().toProfile();
//
//            // convert via jackson mapper
//            HashMap<String, String> profileClaims = mapper.convertValue(profile, stringMapTypeRef);
//            if (profileClaims != null) {
//                claims.add(profileClaims);
//            }
//        }
//
//        return claims;
//    }
//
//    public Map<String, Serializable> getUserClaimsFromResource(UserDetails user, ClientDetails client,
//            Collection<String> scopes, String resourceId) throws NoSuchResourceException {
//        // TODO
//        return Collections.emptyMap();
//    }
//
//    public Map<String, Serializable> getClientClaimsFromResource(ClientDetails client, Collection<String> scopes,
//            String resourceId) throws NoSuchResourceException {
//        // TODO
//        return Collections.emptyMap();
//    }

    /*
     * Execute claim Mapping
     */

    private Map<String, Serializable> executeClaimMapping(String mappingFunction, Map<String, Serializable> claims)
            throws InvalidDefinitionException, SystemException {
        if (!StringUtils.hasText(mappingFunction)) {
            return new HashMap<>(claims);
        }

        // cleanup
        String code = mappingFunction.replaceAll("\\R+", " ");

        return executionService.executeFunction(CLAIM_MAPPING_FUNCTION, code, claims);

    }

}
