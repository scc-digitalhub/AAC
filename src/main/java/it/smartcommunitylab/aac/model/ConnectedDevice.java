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

package it.smartcommunitylab.aac.model;

import it.smartcommunitylab.aac.scope.Scope;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotBlank;

public class ConnectedDevice {

    private String id;

    @NotBlank
    private String subjectId;

    @NotBlank
    private String clientId;

    @NotBlank
    private String realm;

    private String appName;
    private String appDescription;

    private List<Scope> scopes;

    private Date expiration;

    public ConnectedDevice() {}

    public ConnectedDevice(String subjectId, String clientId, String realm, Collection<Scope> scopes) {
        this.subjectId = subjectId;
        this.clientId = clientId;
        this.realm = realm;

        this.scopes = new ArrayList<>();
        this.scopes.addAll(scopes);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppDescription() {
        return appDescription;
    }

    public void setAppDescription(String appDescription) {
        this.appDescription = appDescription;
    }

    public List<Scope> getScopes() {
        return scopes;
    }

    public void setScopes(List<Scope> scopes) {
        this.scopes = scopes;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }
}
