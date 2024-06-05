/**
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.clients.model.ClientDetails;
import it.smartcommunitylab.aac.clients.model.ClientStatus;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/*
 * A model describing the client outside the auth/security context.
 *
 * In addition to core properties, client resources can be included.
 */

@Getter
@Setter
@ToString
@JsonInclude(Include.NON_NULL)
public class Client implements ClientResource, ClientResourceContext {

    @NotBlank
    private final String clientId;

    @NotBlank
    private String realm;

    // basic info
    private String name;
    private String type;

    // status
    private ClientStatus status;

    // audit
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date createDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date modifiedDate;

    // authorities in AAC
    // these are either global or realm scoped
    // stored here because these are NOT resources
    private Set<GrantedAuthority> authorities;

    // resources stored as map context and read via accessors
    @JsonIgnore
    private Map<String, List<ClientResource>> resources = new HashMap<>();

    public Client(@JsonProperty("clientId") String clientId, @JsonProperty("realm") String realm) {
        Assert.hasText(clientId, "clientId can not be null or empty");
        Assert.notNull(realm, "realm can not be null");

        this.clientId = clientId;
        this.realm = realm;
        this.authorities = Collections.emptySet();
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_AAC;
    }

    @Override
    public String getProvider() {
        return SystemKeys.AUTHORITY_AAC;
    }

    @Override
    public String getId() {
        return clientId;
    }

    /*
     * Resource context
     */

    @JsonAnyGetter
    @Override
    public Map<String, List<ClientResource>> getResources() {
        if (this.resources == null) {
            this.resources = new HashMap<>();
        }

        return resources;
    }

    @JsonAnySetter
    public void setResources(Map<String, List<ClientResource>> resources) {
        this.resources = resources;
    }

    public static Client from(ClientDetails details) {
        Assert.notNull(details, "user details can not be null");
        Client client = new Client(details.getClientId(), details.getRealm());
        client.setName(details.getName());
        client.setAuthorities(new HashSet<>(details.getAuthorities()));

        return client;
    }
}
