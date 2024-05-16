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

package it.smartcommunitylab.aac.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.smartcommunitylab.aac.SystemKeys;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.springframework.util.StringUtils;

/*
 * A service defines an api composed of a namespace (used as audience)
 */

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Service {

    @Size(max = 128)
    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String serviceId;

    @Size(max = 128)
    private String realm;

    private String name;
    private String description;

    @NotBlank
    @Pattern(regexp = SystemKeys.NAMESPACE_PATTERN)
    private String namespace;

    @JsonIgnore
    private Map<String, String> claimMapping = new HashMap<>();

    private Collection<ServiceScope> scopes = Collections.emptyList();
    private Collection<ServiceClaim> claims = Collections.emptyList();

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
    
    public String getId() {
    	return this.serviceId;
    }

    @JsonProperty("claimMapping")
    public Map<String, String> getClaimMappingBase64() {
        if (claimMapping == null) {
            return null;
        }
        return claimMapping
            .entrySet()
            .stream()
            .filter(e -> StringUtils.hasText(e.getValue()))
            .collect(
                Collectors.toMap(
                    e -> e.getKey(),
                    e -> {
                        return Base64.getEncoder().encodeToString(e.getValue().getBytes());
                    }
                )
            );
    }

    @JsonProperty("claimMapping")
    public void setClaimMappingBase64(Map<String, String> claimMapping) {
        if (claimMapping != null) {
            this.claimMapping =
                claimMapping
                    .entrySet()
                    .stream()
                    .filter(e -> StringUtils.hasText(e.getValue()))
                    .collect(
                        Collectors.toMap(
                            e -> e.getKey(),
                            e -> {
                                return new String(Base64.getDecoder().decode(e.getValue().getBytes()));
                            }
                        )
                    );
        }
    }

    @JsonIgnore
    public String getUserClaimMapping() {
        if (claimMapping != null && claimMapping.containsKey("user")) {
            return claimMapping.get("user");
        }

        return null;
    }

    @JsonIgnore
    public String getClientClaimMapping() {
        if (claimMapping != null && claimMapping.containsKey("client")) {
            return claimMapping.get("client");
        }

        return null;
    }

    @JsonIgnore
    public String getScopeClaimMapping(String scope) {
        if (claimMapping != null && claimMapping.containsKey(scope)) {
            return claimMapping.get(scope);
        }

        return null;
    }

    @JsonIgnore
    public String getUserClaimMapping(String scope) {
        if (claimMapping != null && claimMapping.containsKey("user:" + scope)) {
            return claimMapping.get("user:" + scope);
        }

        return null;
    }

    @JsonIgnore
    public String getClientClaimMapping(String scope) {
        if (claimMapping != null && claimMapping.containsKey("client:" + scope)) {
            return claimMapping.get("client:" + scope);
        }

        return null;
    }
}
