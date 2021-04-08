package it.smartcommunitylab.aac.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.Claim;
import it.smartcommunitylab.aac.scope.Scope;

/*
 * A service defines an api composed of a namespace (used as audience)
 */

@Valid
public class Service {

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String serviceId;

    @NotBlank
    private String realm;

    private String name;
    private String description;

    @NotBlank
    @Pattern(regexp = SystemKeys.NAMESPACE_PATTERN)
    private String namespace;

    private Map<String, String> claimMapping = new HashMap<>();

    private Collection<ServiceScope> scopes;
    private Collection<ServiceClaim> claims;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Map<String, String> getClaimMapping() {
        return claimMapping;
    }

    public void setClaimMapping(Map<String, String> claimMapping) {
        this.claimMapping = claimMapping;
    }

    public Collection<ServiceScope> getScopes() {
        return scopes;
    }

    public void setScopes(Collection<ServiceScope> scopes) {
        this.scopes = scopes;
    }

    public Collection<ServiceClaim> getClaims() {
        return claims;
    }

    public void setClaims(Collection<ServiceClaim> claims) {
        this.claims = claims;
    }

    public String getUserClaimMapping() {
        if (claimMapping != null && claimMapping.containsKey("user")) {
            return claimMapping.get("user");
        }

        return null;
    }

    public String getClientClaimMapping() {
        if (claimMapping != null && claimMapping.containsKey("client")) {
            return claimMapping.get("client");
        }

        return null;
    }

    public String getScopeClaimMapping(String scope) {
        if (claimMapping != null && claimMapping.containsKey(scope)) {
            return claimMapping.get(scope);
        }

        return null;
    }

    public String getUserClaimMapping(String scope) {
        if (claimMapping != null && claimMapping.containsKey("user:" + scope)) {
            return claimMapping.get("user:" + scope);
        }

        return null;
    }

    public String getClientClaimMapping(String scope) {
        if (claimMapping != null && claimMapping.containsKey("client:" + scope)) {
            return claimMapping.get("client:" + scope);
        }

        return null;
    }
}
