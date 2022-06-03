package it.smartcommunitylab.aac.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.claims.Claim;
import it.smartcommunitylab.aac.claims.ClaimsSet;
import it.smartcommunitylab.aac.claims.DefaultClaimsSet;
import it.smartcommunitylab.aac.claims.ResourceClaimsExtractor;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.claims.model.AbstractClaim;
import it.smartcommunitylab.aac.claims.model.BooleanClaim;
import it.smartcommunitylab.aac.claims.model.DateClaim;
import it.smartcommunitylab.aac.claims.model.NumberClaim;
import it.smartcommunitylab.aac.claims.model.SerializableClaim;
import it.smartcommunitylab.aac.claims.model.StringClaim;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.model.User;

public class ScriptServiceClaimExtractor implements ResourceClaimsExtractor {
    public static final String CLAIM_MAPPING_FUNCTION = "claimMapping";

    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<HashMap<String, Serializable>> serMapTypeRef = new TypeReference<HashMap<String, Serializable>>() {
    };
    private final TypeReference<ArrayList<Serializable>> serListTypeRef = new TypeReference<ArrayList<Serializable>>() {
    };
    private final Service service;
    private ScriptExecutionService executionService;

    // TODO add a loadingcache for scripts

    public ScriptServiceClaimExtractor(Service service) {
        Assert.notNull(service, "services is required");
        this.service = service;
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    public String getResourceId() {
        // service namespace is resourceId
        return service.getNamespace();
    }

    @Override
    public String getRealm() {
//        // we return null to avoid user translation, we do it ourselves
//        return null;
        return service.getRealm();
    }

    public ClaimsSet extractUserClaims(String resourceId, User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions)
            throws InvalidDefinitionException, SystemException {

        if (executionService == null) {
            return null;
        }

        if (!service.getNamespace().equals(resourceId)) {
            throw new IllegalArgumentException("resource id mismatch");
        }

        // fetch claimMapping
        String claimMapping = service.getUserClaimMapping();
        if (!StringUtils.hasText(claimMapping)) {
            return null;
        }

        Map<String, Serializable> exts = Collections.emptyMap();
        if (extensions != null) {
            exts = extensions;
        }

        // translate user, client and scopes to a map
        Map<String, Serializable> map = buildUserContext(user, client, scopes, exts);

        // execute script
        Map<String, Serializable> customClaims = executionService.executeFunction(CLAIM_MAPPING_FUNCTION,
                claimMapping,
                map);

        // map to defined claims and build claimsSet
        List<Claim> claims = processClaims(service.getClaims(), customClaims);

        // build a claimsSet
        DefaultClaimsSet claimsSet = new DefaultClaimsSet();
        claimsSet.setResourceId(resourceId);
        claimsSet.setScope(null);
        claimsSet.setNamespace(resourceId);
        claimsSet.setUser(true);
        claimsSet.setClaims(claims);

        return claimsSet;

    }

    public ClaimsSet extractClientClaims(String resourceId, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions)
            throws InvalidDefinitionException, SystemException {

        if (executionService == null) {
            return null;
        }

        if (!service.getNamespace().equals(resourceId)) {
            throw new IllegalArgumentException("resource id mismatch");
        }

        // fetch claimMapping
        String claimMapping = service.getClientClaimMapping();
        if (!StringUtils.hasText(claimMapping)) {
            return null;
        }

        Map<String, Serializable> exts = Collections.emptyMap();
        if (extensions != null) {
            exts = extensions;
        }

        // translate user, client and scopes to a map
        Map<String, Serializable> map = buildClientContext(client, scopes, exts);

        // execute script
        Map<String, Serializable> customClaims = executionService.executeFunction(CLAIM_MAPPING_FUNCTION,
                claimMapping,
                map);

        // map to defined claims and build claimsSet
        List<Claim> claims = processClaims(service.getClaims(), customClaims);

        // build a claimsSet
        DefaultClaimsSet claimsSet = new DefaultClaimsSet();
        claimsSet.setResourceId(resourceId);
        claimsSet.setScope(null);
        claimsSet.setNamespace(resourceId);
        claimsSet.setUser(true);
        claimsSet.setClaims(claims);

        return claimsSet;

    }

    public Map<String, Serializable> buildUserContext(User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        // clone user and reset content to visible
        User u = new User(user);
        u.setIdentities(null);

        Stream<UserAttributes> iattributes = user.getIdentities().stream()
                .map(i -> i.getAttributes()).flatMap(Collection::stream)
                .filter(a -> isAllowed(scopes, a.getIdentifier()));
        List<UserAttributes> attributes = Stream.concat(iattributes, user.getAttributes(false).stream())
                .collect(Collectors.toList());
        u.setAttributes(attributes);

        // translate user, client and scopes to a map
        Map<String, Serializable> map = new HashMap<>();
        map.put("scopes", new ArrayList<>(scopes));
        map.put("user", mapper.convertValue(u, serMapTypeRef));
        map.put("client", mapper.convertValue(client, serMapTypeRef));
        if (extensions != null) {
            map.put("extensions", mapper.convertValue(extensions, serMapTypeRef));
        }

        return map;
    }

    public Map<String, Serializable> buildClientContext(ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {

        // translate client and scopes to a map
        Map<String, Serializable> map = new HashMap<>();
        map.put("scopes", new ArrayList<>(scopes));
        map.put("client", mapper.convertValue(client, serMapTypeRef));
        if (extensions != null) {
            map.put("extensions", mapper.convertValue(extensions, serMapTypeRef));
        }

        return map;
    }

    private List<Claim> processClaims(Collection<ServiceClaim> serviceClaimsList,
            Map<String, Serializable> customClaims) {
        List<Claim> claims = new ArrayList<>();
        Map<String, ServiceClaim> serviceClaims = serviceClaimsList.stream()
                .collect(Collectors.toMap(sc -> sc.getKey(), sc -> sc));

        for (Map.Entry<String, Serializable> entry : customClaims.entrySet()) {
            String key = entry.getKey();

            if (serviceClaims.containsKey(key)) {
                // try to parse as definition, or drop
                ServiceClaim model = serviceClaims.get(key);
                if (model.isMultiple()) {
                    // try to parse a collection
                    try {
                        ArrayList<Serializable> lis = mapper.convertValue(entry.getValue(), serListTypeRef);
                        if (lis == null) {
                            throw new IllegalArgumentException();
                        }

                        for (Serializable s : lis) {
                            Claim cm = parseClaim(model, s);
                            if (cm != null) {
                                claims.add(cm);
                            }
                        }

                    } catch (IllegalArgumentException ie) {
                        // single value
                        Claim cm = parseClaim(model, entry.getValue());
                        if (cm != null) {
                            claims.add(cm);
                        }
                    }
                } else {
                    // single value
                    Claim cm = parseClaim(model, entry.getValue());
                    if (cm != null) {
                        claims.add(cm);
                    }
                }

            }
        }

        return claims;
    }

    private Claim parseClaim(ServiceClaim model, Serializable value) {
        AbstractClaim c = null;
        try {
            switch (model.getType()) {
            case BOOLEAN:
                Boolean bvalue = mapper.convertValue(value, Boolean.class);
                c = new BooleanClaim(model.getKey(), bvalue);
                break;
            case NUMBER: {
                Number nvalue = mapper.convertValue(value, Number.class);
                c = new NumberClaim(model.getKey(), nvalue);
                break;
            }
            case STRING:
                String svalue = mapper.convertValue(value, String.class);
                c = new StringClaim(model.getKey(), svalue);
                break;
            case DATE:
                Date dvalue = mapper.convertValue(value, Date.class);
                c = new DateClaim(model.getKey(), dvalue);
                break;
            case OBJECT:
                HashMap<String, Serializable> mvalue = mapper.convertValue(value, serMapTypeRef);
                c = new SerializableClaim(model.getKey(), mvalue);
                break;
            default:

            }
        } catch (Exception e) {
            return null;
        }

        if (c != null) {
            c.setName(model.getName());
            c.setDescription(model.getDescription());
        }

        return c;
    }

    private boolean isAllowed(Collection<String> scopes, String identifier) {
        // evaluate if set is accessible given the scope
        // TODO move logic to service and evaluate dependencies
        String scope = "profile." + identifier + ".me";
        return scopes.contains(scope);
    }

}
