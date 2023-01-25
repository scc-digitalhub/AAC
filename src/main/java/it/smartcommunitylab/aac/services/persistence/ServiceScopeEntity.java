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
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import it.smartcommunitylab.aac.repository.StringBase64Converter;

/**
 * @author raman
 *
 */
@Entity
@Table(name = "service_scope", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "service_id", "scope" }),
        @UniqueConstraint(columnNames = { "realm", "scope" })
})
public class ServiceScopeEntity {

    @Id
    @NotNull
    @Column(name = "scope_id", length = 128, unique = true)
    private String scopeId;

    @NotNull
    @Column(name = "service_id", length = 128)
    private String serviceId;

    @NotNull
    @Column(name = "realm", length = 128)
    private String realm;

    @NotBlank
    @Column(length = 128)
    private String scope;

    // user presentation
    // TODO i18n
    private String name;
    private String description;

    // subject type
    private String approvalType;

    /**
     * Roles required to access the scope
     */
    @Lob
    @Column(name = "approval_roles")
    private String approvalRoles;

    /**
     * Space Roles required to access the scope
     */
    @Deprecated
    @Lob
    @Column(name = "approval_space_roles")
    private String approvalSpaceRoles;

    /**
     * Whether explicit manual approval required
     */
    @Column(name = "approval_manual")
    private Boolean approvalRequired;

    @Lob
    @Column(name = "approval_function")
    @Convert(converter = StringBase64Converter.class)
    private String approvalFunction;

    /*
     * Whether any approval suffices, or we need consensus from all
     */
    @Column(name = "approval_any")
    private Boolean approvalAny;

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

    public String getApprovalType() {
        return approvalType;
    }

    public void setApprovalType(String approvalType) {
        this.approvalType = approvalType;
    }

    public String getApprovalRoles() {
        return approvalRoles;
    }

    public void setApprovalRoles(String approvalRoles) {
        this.approvalRoles = approvalRoles;
    }

    public String getApprovalSpaceRoles() {
        return approvalSpaceRoles;
    }

    public void setApprovalSpaceRoles(String approvalSpaceRoles) {
        this.approvalSpaceRoles = approvalSpaceRoles;
    }

    public String getApprovalFunction() {
        return approvalFunction;
    }

    public void setApprovalFunction(String approvalFunction) {
        this.approvalFunction = approvalFunction;
    }

    public boolean isApprovalRequired() {
        return approvalRequired != null ? approvalRequired.booleanValue() : false;
    }

    public Boolean getApprovalRequired() {
        return approvalRequired;
    }

    public void setApprovalRequired(Boolean approvalRequired) {
        this.approvalRequired = approvalRequired;
    }

    public boolean isApprovalAny() {
        return approvalAny != null ? approvalAny.booleanValue() : false;
    }

    public Boolean getApprovalAny() {
        return approvalAny;
    }

    public void setApprovalAny(Boolean approvalAny) {
        this.approvalAny = approvalAny;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

}
