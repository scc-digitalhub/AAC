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
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;


public class DefaultClaimsService implements ClaimsService, InitializingBean {

    public static final String CLAIM_MAPPING_FUNCTION = "claimMapping";

    // claims that should not be overwritten
    private static final Set<String> REGISTERED_CLAIM_NAMES = JWTClaimsSet.getRegisteredNames();
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

        SYSTEM_CLAIM_NAMES = Collections.unmodifiableSet(n);
    }

    // TODO add spaceRole service

    // TODO add servicesService

    private ExecutionService executionService;

    // object mapper
    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<HashMap<String, String>> stringMapTypeRef = new TypeReference<HashMap<String, String>>() {
    };

    public DefaultClaimsService() {
        mapper.setSerializationInclusion(Include.NON_EMPTY);
    }

    public void setExecutionService(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(executionService, "an execution service is required");

    }

    /*
     * build complete claim mapping according to scopes, resourceIds and custom
     * mapping configured in client hook functions
     * 
     * note that we don't validate scopes against client etc, this should be checked
     * elsewhere
     */
    @Override
    public Map<String, Serializable> getUserClaims(UserDetails user, ClientDetails client, Collection<String> scopes,
            Collection<String> resourceIds)
            throws NoSuchResourceException, InvalidDefinitionException, SystemException {

        Map<String, Serializable> claims = new HashMap<>();

        // base information, could be overwritten by converters
        claims.put("sub", user.getSubjectId());

        if (scopes.contains(Config.SCOPE_PROFILE)) {
            // realm should stay behind scope "profile", if client doesn't match the realm
            // it should ask for this info and be approved
            claims.put("realm", user.getRealm());

            // TODO evaluate an additional ID to mark this specific userDetails (ie subject
            // + the identities loaded) so clients will be able to distinguish identities
            // sets for the same subject. Could be as simple as an hash of all userIds
            // sorted alphabetically
        }

        // process basic scopes from userDetails
        if (scopes.contains(Config.SCOPE_OPENID)) {
            Map<String, Serializable> openIdClaims = this.getUserClaimsFromOpenIdProfile(user, scopes);
            claims.putAll(openIdClaims);
        }

        if (scopes.contains(Config.SCOPE_BASIC_PROFILE)) {
            Map<String, Serializable> profileClaims = this.getUserClaimsFromBasicProfile(user);
            claims.putAll(profileClaims);
        }

        if (scopes.contains(Config.SCOPE_ACCOUNT_PROFILE)) {
            ArrayList<Serializable> accountClaims = this.getUserClaimsFromAccountProfile(user);
            claims.put("accounts", accountClaims);
        }

        // realm role claims
        if (scopes.contains(Config.SCOPE_ROLE)) {
            // TODO read realmRoles from service (need impl)
        }

        // space roles
        // TODO

        // group claims
        // TODO, we need to define groups

        // services can add claims
        Map<String, Serializable> servicesClaims = new HashMap<>();
        // TODO
        Set<String> serviceIds = new HashSet<>();
        serviceIds.addAll(resourceIds);
        // resolve scopes to services to integrate list
        // TODO
        for (String scope : scopes) {
            // TODO resolve if matches a service scope, fetch serviceId
        }

        for (String serviceId : serviceIds) {
            // narrow down userDetails + clientDetails to match service realm
            // TODO
            // call service and let it provide additional data
            // TODO
            HashMap<String, Serializable> serviceClaims = null;
            // enforce prefix via service namespace
            // TODO
            String namespace = serviceId;
            servicesClaims.put(namespace, serviceClaims);
        }

        // integrate, no clash thanks to namespacing
        claims.putAll(servicesClaims);

        // freeze reserved claims by keeping keys, these won't be modifiable
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
                            !SYSTEM_CLAIM_NAMES.contains(e.getKey())))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            claims.putAll(allowedClaims);
        }

        return claims;
    }

    @Override
    public Map<String, Serializable> getClientClaims(ClientDetails client, Collection<String> scopes,
            Collection<String> resourceIds)
            throws NoSuchResourceException, InvalidDefinitionException, SystemException {
        // TODO
        return Collections.emptyMap();
    }

    public Map<String, Serializable> getUserClaimsFromBasicProfile(UserDetails user) {
        Map<String, Serializable> claims = new HashMap<>();

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (!identities.isEmpty()) {
            // TODO decide how to merge identities into a single profile
            // for now get first identity, should be last logged in
            BasicProfile profile = identities.iterator().next().toBasicProfile();

            // convert via jackson mapper
            claims.putAll(mapper.convertValue(profile, stringMapTypeRef));

        }

        return claims;
    }

    public Map<String, Serializable> getUserClaimsFromOpenIdProfile(UserDetails user, Collection<String> scopes) {
        Map<String, Serializable> claims = new HashMap<>();

        OpenIdProfile profile = null;

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (!identities.isEmpty()) {
            // TODO decide how to merge identities into a single profile
            // for now get first identity, should be last logged in
            profile = identities.iterator().next().toOpenIdProfile();

        }

        // build result according to scopes, via narrow down
        if (scopes.contains(Config.SCOPE_PROFILE)) {
            // narrow down and convert via jackson mapper
            claims.putAll(mapper.convertValue(profile.toDefaultProfile(), stringMapTypeRef));
        }
        if (scopes.contains(Config.SCOPE_EMAIL)) {
            // narrow down and convert via jackson mapper
            claims.putAll(mapper.convertValue(profile.toEmailProfile(), stringMapTypeRef));
        }
        if (scopes.contains(Config.SCOPE_ADDRESS)) {
            // narrow down and convert via jackson mapper
            claims.putAll(mapper.convertValue(profile.toAddressProfile(), stringMapTypeRef));
        }
        if (scopes.contains(Config.SCOPE_PHONE)) {
            // narrow down and convert via jackson mapper
            claims.putAll(mapper.convertValue(profile.toPhoneProfile(), stringMapTypeRef));
        }

        return claims;
    }

    public ArrayList<Serializable> getUserClaimsFromAccountProfile(UserDetails user) {
        ArrayList<Serializable> claims = new ArrayList<>();

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        for (UserIdentity identity : identities) {
            // get account and translate
            AccountProfile profile = identity.getAccount().toProfile();

            // convert via jackson mapper
            HashMap<String, String> profileClaims = mapper.convertValue(profile, stringMapTypeRef);
            if (profileClaims != null) {
                claims.add(profileClaims);
            }
        }

        return claims;
    }

    public Map<String, Serializable> getUserClaimsFromResource(UserDetails user, ClientDetails client,
            Collection<String> scopes, String resourceId) throws NoSuchResourceException {
        // TODO
        return Collections.emptyMap();
    }

    public Map<String, Serializable> getClientClaimsFromResource(ClientDetails client, Collection<String> scopes,
            String resourceId) throws NoSuchResourceException {
        // TODO
        return Collections.emptyMap();
    }

    /*
     * Execute claim Mapping
     */

    private Map<String, Serializable> executeClaimMapping(String mappingFunction, Map<String, Serializable> claims)
            throws InvalidDefinitionException, SystemException {
        if (!StringUtils.hasText(mappingFunction)) {
            return new HashMap<>(claims);
        }

        return executionService.executeFunction(CLAIM_MAPPING_FUNCTION, mappingFunction, claims);

    }

}
