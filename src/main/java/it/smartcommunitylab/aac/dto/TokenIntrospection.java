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

package it.smartcommunitylab.aac.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * ITEF RFC7662 Token Introspection model
 * @author raman
 *
 */
@JsonInclude(Include.NON_NULL)
public class TokenIntrospection {

	private boolean active;
	private String scope;
	private String client_id;
	private String username;
	private String token_type, sub, iss, jti, aud;
	private Integer exp, iat, nbf;
	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}
	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}
	/**
	 * @return the scope
	 */
	public String getScope() {
		return scope;
	}
	/**
	 * @param scope the scope to set
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}
	/**
	 * @return the client_id
	 */
	public String getClient_id() {
		return client_id;
	}
	/**
	 * @param client_id the client_id to set
	 */
	public void setClient_id(String client_id) {
		this.client_id = client_id;
	}
	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}
	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	/**
	 * @return the token_type
	 */
	public String getToken_type() {
		return token_type;
	}
	/**
	 * @param token_type the token_type to set
	 */
	public void setToken_type(String token_type) {
		this.token_type = token_type;
	}
	/**
	 * @return the sub
	 */
	public String getSub() {
		return sub;
	}
	/**
	 * @param sub the sub to set
	 */
	public void setSub(String sub) {
		this.sub = sub;
	}
	/**
	 * @return the iss
	 */
	public String getIss() {
		return iss;
	}
	/**
	 * @param iss the iss to set
	 */
	public void setIss(String iss) {
		this.iss = iss;
	}
	/**
	 * @return the jti
	 */
	public String getJti() {
		return jti;
	}
	/**
	 * @param jti the jti to set
	 */
	public void setJti(String jti) {
		this.jti = jti;
	}
	/**
	 * @return the exp
	 */
	public Integer getExp() {
		return exp;
	}
	/**
	 * @param exp the exp to set
	 */
	public void setExp(Integer exp) {
		this.exp = exp;
	}
	/**
	 * @return the iat
	 */
	public Integer getIat() {
		return iat;
	}
	/**
	 * @param iat the iat to set
	 */
	public void setIat(Integer iat) {
		this.iat = iat;
	}
	/**
	 * @return the nbf
	 */
	public Integer getNbf() {
		return nbf;
	}
	/**
	 * @param nbf the nbf to set
	 */
	public void setNbf(Integer nbf) {
		this.nbf = nbf;
	}
	/**
	 * @return the aud
	 */
	public String getAud() {
		return aud;
	}
	/**
	 * @param aud the aud to set
	 */
	public void setAud(String aud) {
		this.aud = aud;
	}
}
