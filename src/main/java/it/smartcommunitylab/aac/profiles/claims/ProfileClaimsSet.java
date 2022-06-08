package it.smartcommunitylab.aac.profiles.claims;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import it.smartcommunitylab.aac.claims.Claim;
import it.smartcommunitylab.aac.claims.ClaimsSet;
import it.smartcommunitylab.aac.claims.model.SerializableClaim;
import it.smartcommunitylab.aac.core.base.AbstractProfile;

public class ProfileClaimsSet implements ClaimsSet {

    public static final String RESOURCE_ID = "aac.profile";

    private String scope;
    private String key;
    private String namespace;

    private AbstractProfile profile;

    public AbstractProfile getProfile() {
        return profile;
    }

    public void setProfile(AbstractProfile profile) {
        this.profile = profile;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getResourceId() {
        return RESOURCE_ID;
    }

    @Override
    public Collection<Claim> getClaims() {
        if (profile == null) {
            return Collections.emptyList();
        }

//        SerializableClaim claim = new SerializableClaim(key);
//        claim.setValue(profile.toMap());
//
//        return Collections.singleton(claim);

        // serialize each property as claim
        List<Claim> claims = new ArrayList<>();
        Map<String, Serializable> map = profile.toMap();
        for (Map.Entry<String, Serializable> e : map.entrySet()) {
            if (!ArrayUtils.contains(SYSTEM_CLAIMS, e.getKey())) {
                SerializableClaim sc = new SerializableClaim(e.getKey(), e.getValue());
                claims.add(sc);
            }
        }

        return claims;
    }

    @Override
    public Map<String, Serializable> exportClaims() {
        if (profile == null) {
            return Collections.emptyMap();
        }

        // get claims
        Collection<Claim> claims = getClaims();

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

    public static final String[] SYSTEM_CLAIMS = {
            "authority", "provider", "realm", "subjectId", "userId", "urn", "id", "resourceId", "profileId"
    };

}
