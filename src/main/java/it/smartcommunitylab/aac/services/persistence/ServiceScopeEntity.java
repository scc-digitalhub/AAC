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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * @author raman
 *
 */
@Entity
@Table(name = "service_scope")
public class ServiceScopeEntity {

    @Id
    private String scope;

    /**
     * ServiceDescriptor ID
     */
//    @ManyToOne(optional = false)
//    @OnDelete(action = OnDeleteAction.CASCADE)
//    @JoinColumn(nullable = false, name = "service_id", referencedColumnName = "service_id")
//    private ServiceEntity service;

    @NotNull
    @Column(name = "service_id")
    private String serviceId;

    /**
     * Human-readable scope name
     */
    private String name;
    /**
     * Resource description
     */
    private String description;

    /**
     * Claims exposed with the scopes (comma-separated list)
     */
    @Column(columnDefinition = "LONGTEXT")
    private String claims;

    /**
     * Roles required to access the scope
     */
    @Column(columnDefinition = "LONGTEXT")
    private String roles;

//    /**
//     * Authority that can access this resource
//     */
//    @Enumerated(EnumType.STRING)
//    private AUTHORITY authority;

    @Column(name = "scope_type")
    private String type;

    /**
     * Whether explicit manual approval required
     */
    private boolean approvalRequired = false;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

//    public ServiceEntity getService() {
//        return service;
//    }
//
//    public void setService(ServiceEntity service) {
//        this.service = service;
//    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
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

    public String getClaims() {
        return claims;
    }

    public void setClaims(String claims) {
        this.claims = claims;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public boolean isApprovalRequired() {
        return approvalRequired;
    }

    public void setApprovalRequired(boolean approvalRequired) {
        this.approvalRequired = approvalRequired;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
