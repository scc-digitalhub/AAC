package it.smartcommunitylab.aac.claims;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class DefaultClaimsSet implements ClaimsSet {

    private String resourceId;
    private String scope;
    private String namespace;

    private boolean isUser = false;
    private boolean isClient = false;

    private List<Claim> claims = Collections.emptyList();

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isUser() {
        return isUser;
    }

    public void setUser(boolean isUser) {
        this.isUser = isUser;
    }

    public boolean isClient() {
        return isClient;
    }

    public void setClient(boolean isClient) {
        this.isClient = isClient;
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public void setClaims(List<Claim> claims) {
        this.claims = claims;
    }

    @Override
    public Map<String, Serializable> exportClaims() {
        if (claims == null) {
            return Collections.emptyMap();
        }

        // translate each claim to serializable, merge multiple keys under collections
        MultiValueMap<String, Serializable> map = new LinkedMultiValueMap<>();
        claims.forEach(claim -> {
            map.add(claim.getKey(), claim.getValue());
        });

        // flatten and collect
        Map<String, Serializable> result = new HashMap<>();
        map.forEach((key, list) -> {
            if (list.size() == 1) {
                result.put(key, list.get(0));
            } else {
                // use arrayList to ensure element is serializable
                result.put(key, new ArrayList<>(list));
            }
        });

        return result;
    }
}
