/**
 *    Copyright 2012-2013 Trento RISE
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
 */

package it.smartcommunitylab.aac.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.api.client.util.Sets;

/**
 * DB entity representing the user: user ID, social ID, and the attributes
 * @author raman
 *
 */
@Entity
@Table(name="user")
public class User implements Serializable {

	private static final long serialVersionUID = 1067996326671906278L;

	@Id
	@GeneratedValue
	private Long id;

//	@OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST,
//			CascadeType.REMOVE, CascadeType.MERGE })
	@OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.ALL})	
	@JoinColumn(name = "USER_ID", nullable=false)
	@JsonIgnore
	private Set<Attribute> attributeEntities;

	@OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.ALL}, orphanRemoval = true)	
	@JoinColumn(name = "USER_ID", nullable=false)
	private Set<Role> roles;	
	
	private String name; 
	private String surname;
	private String fullName;
	
	private String username;
	
	public User() {
		super();
		this.roles = Sets.newHashSet();
	}
	
	
	/**
	 * Create user with the specified parameters
	 * @param id
	 * @param name
	 * @param surname
	 * @param attrs 
	 */
	public User(String name, String surname, HashSet<Attribute> attrs) {
		super();
		updateNames(name, surname);
		this.attributeEntities = attrs;
		this.roles = Sets.newHashSet();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<Attribute> getAttributeEntities() {
		return attributeEntities;
	}

	public void setAttributeEntities(Set<Attribute> attributeEntities) {
		this.attributeEntities = attributeEntities;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	@Override
	public String toString() {
		return name + " " + surname;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getFullName() {
		if (fullName == null) {
			fullName = surname + " "+ name;
		}
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}


	/**
	 * Update name/surname params
	 * @param name
	 * @param surname
	 */
	public void updateNames(String name, String surname) {
		if (name != null) setName(name);
		if (surname != null) setSurname(surname);
		setFullName((getName()+" "+getSurname()).trim().toLowerCase());
	}
	
	public boolean hasRole(String role, String context, String space) {
		return roles
		.stream().anyMatch(r -> {
			return 	   role.equals(r.getRole()) 
					&& (context == null && r.getContext() == null || context != null && context.equals(r.getContext()))
					&& (space == null && r.getSpace() == null || space != null && space.equals(r.getSpace()));
		});
	}
	
	/**
	 * Get all the SpaceRoles at the specified context
	 * @param role
	 * @param context
	 * @return
	 */
	public Set<Role> contextRole(String role, String context) {
		return roles
		.stream()
		.filter(r -> r.getRole().equals(role) && (context == null && r.getContext() == null || context != null && context.equals(r.getContext())) )
		.collect(Collectors.toSet());
	}
	
	/**
	 * Get all the SpaceRoles with the specified role value
	 * @param role
	 * @param context
	 * @return
	 */
	public Set<Role> contextRole(String role) {
		return roles
		.stream()
		.filter(r -> r.getRole().equals(role))
		.collect(Collectors.toSet());
	}
	/**
	 * Get all the SpaceRoles at the specified space
	 * @param context
	 * @param space
	 * @return
	 */
	public Set<Role> spaceRole(String context, String space) {
		return roles
		.stream()
		.filter(r -> (space == null && r.getSpace() == null || space != null && space.equals(r.getSpace())) && (context == null && r.getContext() == null || context != null && context.equals(r.getContext())) )
		.collect(Collectors.toSet());
	}
	
	public String attributeValue(String authority, String key) {
		Optional<Attribute> attr = attributeEntities
				.stream()
				.filter(a -> {return a.getAuthority().getName().equals(authority) && a.getKey().equals(key);})
				.findAny();
		if (attr.isPresent()) return attr.get().getValue();
		return null;
	}

}
