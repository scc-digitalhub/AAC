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

package it.smartcommunitylab.aac.claims;

import java.util.Collections;
import java.util.List;

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
}
