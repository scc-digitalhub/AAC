/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.profiles.claims;

import it.smartcommunitylab.aac.claims.Claim;
import it.smartcommunitylab.aac.claims.ClaimsSet;
import it.smartcommunitylab.aac.claims.model.SerializableClaim;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProfileClaimsSet implements ClaimsSet {

    public static final String RESOURCE_ID = "aac.profile";

    private String scope;
    private String key;
    private String namespace;
    private boolean isUser;

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

    public void setUser(boolean isUser) {
        this.isUser = isUser;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public boolean isUser() {
        return isUser;
    }

    @Override
    public boolean isClient() {
        return !isUser;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    //    @Override
    //    public Map<String, Serializable> getClaims() {
    //        if (profile == null) {
    //            return Collections.emptyMap();
    //        }
    //
    //        return profile.toMap();
    //    }

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
            SerializableClaim sc = new SerializableClaim(e.getKey(), e.getValue());
            // key is namespace
            sc.setNamespace(getKey());
            claims.add(sc);
        }

        return claims;
    }
}
