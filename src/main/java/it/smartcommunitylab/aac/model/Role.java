package it.smartcommunitylab.aac.model;

import it.smartcommunitylab.aac.manager.RoleManager.ROLE;
import it.smartcommunitylab.aac.manager.RoleManager.SCOPE;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "role")
public class Role implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5989316043413301379L;

	@Id
	@GeneratedValue
	private Long id;

	private SCOPE scope;
	private ROLE role;
	private String context;

	public Role() {
	}
	
	public Role(SCOPE scope, ROLE role, String context) {
		super();
		this.scope = scope;
		this.role = role;
		this.context = context;
	}
	
	public static Role systemUser() {
		return new Role(SCOPE.system, ROLE.user, null);
	}


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public SCOPE getScope() {
		return scope;
	}

	public void setScope(SCOPE scope) {
		this.scope = scope;
	}

	public ROLE getRole() {
		return role;
	}

	public void setRole(ROLE role) {
		this.role = role;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		result = prime * result + ((scope == null) ? 0 : scope.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Role other = (Role) obj;
		if (context == null) {
			if (other.context != null)
				return false;
		} else if (!context.equals(other.context))
			return false;
		if (role != other.role)
			return false;
		if (scope != other.scope)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return scope + " " + role + (context == null ? "" : " - " + context) + " [" + id + "]";
	}

}
