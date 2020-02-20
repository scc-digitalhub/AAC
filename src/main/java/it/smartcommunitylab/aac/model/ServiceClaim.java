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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config.CLAIM_TYPE;

/**
 * @author raman
 *
 */
@Entity
@Table(name="service_claim")
public class ServiceClaim {

	private static ObjectMapper mapper = new ObjectMapper();
	
	@Id
	@GeneratedValue
	private Long claimId;
	
	private String claim;
	
	/**
	 * ServiceDescriptor ID
	 */
	@ManyToOne(optional=false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(nullable=false, name="serviceId",referencedColumnName="serviceId")
	private Service service;

	/**
	 * Human-readable claim name
	 */
	private String name;

	/**
	 * If claim type is multiple
	 */
	private boolean multiple;
	
	@Enumerated(EnumType.STRING)
	private CLAIM_TYPE type;

	/**
	 * @return the claimId
	 */
	public Long getClaimId() {
		return claimId;
	}

	/**
	 * @param claimId the claimId to set
	 */
	public void setClaimId(Long claimId) {
		this.claimId = claimId;
	}

	/**
	 * @return the service
	 */
	public Service getService() {
		return service;
	}

	/**
	 * @param service the service to set
	 */
	public void setService(Service service) {
		this.service = service;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the multiple
	 */
	public boolean isMultiple() {
		return multiple;
	}

	/**
	 * @param multiple the multiple to set
	 */
	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	/**
	 * @return the type
	 */
	public CLAIM_TYPE getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(CLAIM_TYPE type) {
		this.type = type;
	}

	public String getClaim() {
		return claim;
	}

	public void setClaim(String claim) {
		this.claim = claim;
	}
	
	/**
	 * Fully qualified claim name (namespace / local claim)
	 * @param namespace
	 * @param claim
	 * @return
	 */
	public static String qualifiedName(String namespace, String claim) {
		return namespace == null ? claim : String.format("%s/%s", namespace, claim);
	}
	/**
	 * Check claim type
	 * @param value
	 * @param multiple
	 * @param type
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static boolean ofType(Object value, boolean multiple, CLAIM_TYPE type) {
		if (value == null) return true;
		boolean vMultiple = (value.getClass().isArray() || value instanceof Collection);
		if (vMultiple != multiple) {
			return false;
		}
		Object v = value.getClass().isArray() 
				? ((Object[])value).length > 0 ? ((Object[])value)[0] : null
				: (value instanceof Collection) 
				? ((Collection)value).size() > 0 ? ((Collection)value).iterator().next() : null
				: value;
		
		if (v == null) return true;
		
		if (type.equals(CLAIM_TYPE.type_number)) {
			try {
				Double.parseDouble(v.toString());
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		if (type.equals(CLAIM_TYPE.type_boolean)) {
			try {
				Boolean.parseBoolean(v.toString());
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
				
		if (v instanceof String && type.equals(CLAIM_TYPE.type_string) ||
			v instanceof Map && type.equals(CLAIM_TYPE.type_object)) {
			return true;
		}
		
		return false;
	}

	/**
	 * Return typed value of the claim parsing the string representation
	 * @param claim
	 * @param value
	 * @return
	 */
	public static Object typedValue(ServiceClaim claim, String value) {
		try {
			if (claim.isMultiple()) return mapper.readValue(value, LinkedList.class);
			switch (claim.getType()) {
			case type_boolean: return Boolean.parseBoolean(value);
			case type_number: {
				try {
					return Integer.parseInt(value);
				} catch (Exception e) {
					return Double.parseDouble(value);
				}
			}
			case type_object: return mapper.readValue(value, HashMap.class);
			default: return value; 
			}
		} catch (Exception e) {
			return value;
		}
	}
	
}
