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

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author raman
 *
 */
@Entity
@Table(name="registration")
public class Registration implements Serializable {
	
	private static final long serialVersionUID = 5151437264220742574L;

	@Id
	@GeneratedValue
	private Long id;
	
	@Column(unique=true)
	private String email;
	private String name;
	private String surname;

	private String lang;
	
	private String password;
	
	private boolean confirmed;
	private Date confirmationDeadline;
	
	private String confirmationKey;
	
	private String userId;
	
	@Column(name="change_first_access")
	private Boolean changeOnFirstAccess;
	
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}
	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
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
	 * @return the surname
	 */
	public String getSurname() {
		return surname;
	}
	/**
	 * @param surname the surname to set
	 */
	public void setSurname(String surname) {
		this.surname = surname;
	}
	/**
	 * @return the confirmed
	 */
	public boolean isConfirmed() {
		return confirmed;
	}
	/**
	 * @param confirmed the confirmed to set
	 */
	public void setConfirmed(boolean confirmed) {
		this.confirmed = confirmed;
	}
	/**
	 * @return the confirmationDeadline
	 */
	public Date getConfirmationDeadline() {
		return confirmationDeadline;
	}
	/**
	 * @param confirmationDeadline the confirmationDeadline to set
	 */
	public void setConfirmationDeadline(Date confirmationDeadline) {
		this.confirmationDeadline = confirmationDeadline;
	}
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * @return the confirmationKey
	 */
	public String getConfirmationKey() {
		return confirmationKey;
	}
	/**
	 * @param confirmationKey the confirmationKey to set
	 */
	public void setConfirmationKey(String confirmationKey) {
		this.confirmationKey = confirmationKey;
	}
	/**
	 * @return the lang
	 */
	public String getLang() {
		return lang;
	}
	/**
	 * @param lang the lang to set
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}
	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public Boolean isChangeOnFirstAccess() {
		return changeOnFirstAccess != null && changeOnFirstAccess;
	}
	public void setChangeOnFirstAccess(Boolean changeOnFirstAccess) {
		this.changeOnFirstAccess = changeOnFirstAccess;
	}

	
}
