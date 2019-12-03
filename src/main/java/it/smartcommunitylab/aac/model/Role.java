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

package it.smartcommunitylab.aac.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import it.smartcommunitylab.aac.Config;

/**
 * @author raman
 *
 */
@Entity
@Table(name="space_role")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Role implements GrantedAuthority {

	private static final long serialVersionUID = -3036514846005728961L;

	@Id
	@GeneratedValue
	private Long id;
	
	private String context;
	private String space;
	private String role;
	
	public Role() {
		super();
	}

	public Role(String context, String space, String role) {
		super();
		this.context = context;
		this.space = space;
		this.role = role;
		validate(this);
	}


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getSpace() {
		return space;
	}

	public void setSpace(String space) {
		this.space = space;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public String toString() {
		return "Role [id=" + id + ", context=" + context + ", space=" + space + ", role=" + role + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		result = prime * result + ((space == null) ? 0 : space.hashCode());
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
		if (role == null) {
			if (other.role != null)
				return false;
		} else if (!role.equals(other.role))
			return false;
		if (space == null) {
			if (other.space != null)
				return false;
		} else if (!space.equals(other.space))
			return false;
		return true;
	}


	public String canonicalSpace() {
		StringBuilder sb = new StringBuilder();
		if (!StringUtils.isEmpty(context)) {
			sb.append(context);
			sb.append('/');
		}
		if (!StringUtils.isEmpty(space)) {
			sb.append(space);
		}
		return sb.toString();
	}

	@Override
	public String getAuthority() {
		StringBuilder sb = new StringBuilder();
		if (!StringUtils.isEmpty(context)) {
			sb.append(context);
			sb.append('/');
		}
		if (!StringUtils.isEmpty(space)) {
			sb.append(space);
			sb.append(':');
		}
		sb.append(role);
		return sb.toString();
	}

	/**
	 * @return
	 */
	public static Role systemUser() {
		return new Role(null, null, Config.R_USER);
	}

	/**
	 * @return
	 */
	public static Role systemAdmin() {
		return new Role(null, null, Config.R_ADMIN);
	}

	/**
	 * @param ctxStr
	 * @return
	 */
	public static Role ownerOf(String ctxStr) {
		int idx = ctxStr.lastIndexOf('/');
		String ctx = idx > 0 ? ctxStr.substring(0,  idx) : null;
		String space = idx > 0 ? ctxStr.substring(idx + 1) : ctxStr;
		return new Role(ctx, space, Config.R_PROVIDER);
	}

	/**
	 * @param ctxStr
	 * @return
	 */
	public static Role memberOf(String ctxStr, String role) {
		int idx = ctxStr.lastIndexOf('/');
		String ctx = idx > 0 ? ctxStr.substring(0,  idx) : null;
		String space = idx > 0 ? ctxStr.substring(idx + 1) : ctxStr;
		Role r = new Role(ctx, space, role);
		validate(r);
		return r;
	}

	/**
	 * @param x
	 * @return
	 */
	public static Role parse(String s) throws IllegalArgumentException {
		s = s.trim();
		int idx = s.lastIndexOf(':');
		if (StringUtils.isEmpty(s) || idx == s.length() - 1) throw new IllegalArgumentException("Invalid Role format " + s);
		if (idx <= 0) return new Role(null, null, s.substring(idx + 1));
		return memberOf(s.substring(0, idx), s.substring(idx + 1));
	}

	public static void validate(Role r) throws IllegalArgumentException {
		// context may be empty
		if (r.context != null && !r.context.matches("[\\w\\./]+")) {
			throw new IllegalArgumentException("Invalid role context value: only alpha-numeric characters and '_./' allowed");
		};
		// space empty only if context is empty
		if (r.space == null && r.context != null || r.space != null && !r.space.matches("[\\w\\.]+")) {
			throw new IllegalArgumentException("Invalid role space value: only alpha-numeric characters and '_.' allowed");
		};
		// role should never be empty
		if (r.role == null || !r.role.matches("[\\w\\.]+")) {
			throw new IllegalArgumentException("Invalid role value: only alpha-numeric characters and '_.' allowed");
		};
	}

}
