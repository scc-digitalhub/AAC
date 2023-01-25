/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.services.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

/**
 * @author raman
 *
 */
@Entity
@Table(name = "service_claim", uniqueConstraints = @UniqueConstraint(columnNames = { "service_id", "claim_key" }))
public class ServiceClaimEntity {

    @Id
    @NotNull
    @Column(name = "claim_id", length = 128)
    private String claimId;

    @NotNull
    @Column(name = "service_id", length = 128)
    private String serviceId;

    @NotNull
    @Column(name = "realm", length = 128)
    private String realm;

    @NotNull
    @Column(name = "claim_key", length = 128)
    private String key;

//  @Enumerated(EnumType.STRING)
//  private AttributeType type;

    @NotNull
    @Column(name = "claim_type", length = 32)
    private String type;

    @Column(name = "claim_multiple")
    private Boolean multiple;

    /**
     * Human-readable claim name
     */
    private String name;
    private String description;

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

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

    public boolean isMultiple() {
        return multiple != null ? multiple.booleanValue() : false;
    }

    public Boolean getMultiple() {
        return multiple;
    }

    public void setMultiple(Boolean multiple) {
        this.multiple = multiple;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
