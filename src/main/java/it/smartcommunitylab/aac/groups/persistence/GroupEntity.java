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

package it.smartcommunitylab.aac.groups.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author raman
 *
 */
@Entity
@Table(name = "groups", uniqueConstraints = @UniqueConstraint(columnNames = { "realm", "group_key" }))
@EntityListeners(AuditingEntityListener.class)
public class GroupEntity {

    public static final String ID_PREFIX = "g_";

    /*
     * UUID is system-wide auto-generated id
     */
    @Id
    @NotNull
    @Column(length = 128, unique = true)
    private String uuid;

    @NotNull
    @Column(length = 128)
    private String realm;

    /*
     * group is user-defined key, unique per realm
     */
    @NotNull
    @Column(name = "group_key", length = 128, unique = true)
    private String group;

    /*
     * parent group supports hierarchy, refers to group key
     */
    @Column(name = "parent_group", length = 128)
    private String parentGroup;

    private String name;
    private String description;

    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date modifiedDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getParentGroup() {
        return parentGroup;
    }

    public void setParentGroup(String parentGroup) {
        this.parentGroup = parentGroup;
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

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
}
