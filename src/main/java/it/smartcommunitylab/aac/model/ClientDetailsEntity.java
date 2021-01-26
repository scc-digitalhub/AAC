/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.Utils;

/**
 * DB entity storing the client app information
 * @author raman
 *
 */
@Entity
@Table(name = "oauth_client_details", uniqueConstraints = @UniqueConstraint(columnNames = { "name", "developerId" }))
public class ClientDetailsEntity implements ClientDetails {
	private static final long serialVersionUID = -286007838648327741L;

	private static final String KEY_APP_SIGNATURES = "app_signature";
	
	private static ObjectMapper mapper = new ObjectMapper();

//	@Column(name = "name", unique=true)
    @Column(name = "name")	
	private String name;	
	
	@Column(name = "client_id", unique=true)
	private String clientId;
	
	@Column(name = "client_secret",nullable = false)
	private String clientSecret;

	   //DEPRECATED legacy Native Flow
//	@Column(name = "client_secret_mobile",nullable = false)
//	private String clientSecretMobile;

    //DEPRECATED legacy Native Flow
//	@Column(name = "mobile_app_schema", unique=true,columnDefinition="LONGTEXT")
//	private String mobileAppSchema; 
	
	@Column(name = "resource_ids",columnDefinition="LONGTEXT")
	private String resourceIds;
	
	@Column(name = "scope",columnDefinition="LONGTEXT")
	private String scope;

	@Column(name = "authorized_grant_types")
	private String authorizedGrantTypes;

	@Column(name = "web_server_redirect_uri",columnDefinition="LONGTEXT")
	private String redirectUri;

	@Column(name = "authorities")
	private String authorities;

	@Column(name = "access_token_validity")
	private Integer accessTokenValidity;

	@Column(name = "refresh_token_validity")
	private Integer refreshTokenValidity;

	@Column(name = "additional_information",columnDefinition="LONGTEXT")
	private String additionalInformation;

	@Column(name = "autoapprove")
	private String autoapprove;

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private Long developerId;
	
	@Column(name = "parameters",columnDefinition="LONGTEXT")
	private String parameters;	
	

	// TODO: client metadata as of RFC 7591
	// - client_id	 				-> client_id
	// - client_secret 				-> client_secret
	// - redirect_uris 				-> redirectUri (array of strings)
	// - token_endpoint_auth_method	-> TODO (string)
	// - grant_types				-> authorizedGrantTypes (array of strings)
	// - response_types 			-> TODO  (array of strings)
	// - client_name				-> name (string)
	// - client_uri					-> TODO (string)
	// - logo_uri					-> TODO (string)
	// - scope						-> scope (string space separated)
	// - contacts					-> TODO (array of strings, e.g., emails)
	// - tos_uri					-> TODO (string)
	// - policy_uri					-> TODO (string)
	// - jwks_uri					-> TODO (string)
	// - jwks						-> TODO (document)
	// - software_id				-> TODO (string)
	// - software_version			-> TODO (string)
	// - client_id_issued_at		-> TODO (seconds)
	// - client_secret_expires_at	-> TODO (seconds)

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the clientId
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @param clientId the clientId to set
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * @param resourceIds the resourceIds to set
	 */
	public void setResourceIds(String resourceIds) {
		this.resourceIds = resourceIds;
	}

	/**
	 * @param scope the scope to set
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}

	/**
	 * @param authorizedGrantTypes the authorizedGrantTypes to set
	 */
	public void setAuthorizedGrantTypes(String authorizedGrantTypes) {
		this.authorizedGrantTypes = authorizedGrantTypes;
	}

	/**
	 * @return the redirectUri
	 */
	public String getRedirectUri() {
		return redirectUri;
	}

	/**
	 * @param redirectUri the redirectUri to set
	 */
	public void setRedirectUri(String redirectUri) {
		this.redirectUri = redirectUri;
	}
	/**
	 * @param authorities the authorities to set
	 */
	public void setAuthorities(String authorities) {
		this.authorities = authorities;
	}

	/**
	 * @return the accessTokenValidity
	 */
	public Integer getAccessTokenValidity() {
		return accessTokenValidity;
	}

	/**
	 * @param accessTokenValidity the accessTokenValidity to set
	 */
	public void setAccessTokenValidity(Integer accessTokenValidity) {
		this.accessTokenValidity = accessTokenValidity;
	}

	/**
	 * @return the refreshTokenValidity
	 */
	public Integer getRefreshTokenValidity() {
		return refreshTokenValidity;
	}

	/**
	 * @param refreshTokenValidity the refreshTokenValidity to set
	 */
	public void setRefreshTokenValidity(Integer refreshTokenValidity) {
		this.refreshTokenValidity = refreshTokenValidity;
	}

	/**
	 * @param clientSecret the clientSecret to set
	 */
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	   //DEPRECATED legacy Native Flow
//	/**
//	 * @return {@link #clientSecretMobile} value
//	 */
//	public String getClientSecretMobile() {
//		return clientSecretMobile;
//	}
//
//	/**
//	 * 
//	 * @param clientSecretMobile value to set
//	 */
//	public void setClientSecretMobile(String clientSecretMobile) {
//		this.clientSecretMobile = clientSecretMobile;
//	}

	/**
	 * @param additionalInformation the additionalInformation to set
	 */
	public void setAdditionalInformation(String additionalInformation) {
		this.additionalInformation = additionalInformation;
	}

	/**
	 * @return the developerId
	 */
	public Long getDeveloperId() {
		return developerId;
	}

	/**
	 * @param developerId the developerId to set
	 */
	public void setDeveloperId(Long developerId) {
		this.developerId = developerId;
	}

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

	@Override
	public Set<String> getResourceIds() {
		if (resourceIds != null) {
			Set<String> set = Utils.delimitedStringToSet(resourceIds, ",");
			set.remove("");
			return set;
		}
		return Collections.emptySet();
	}

	@Override
	public boolean isSecretRequired() {
		return true;
	}

	@Override
	public String getClientSecret() {
		return clientSecret;
	}

	@Override
	public boolean isScoped() {
		return scope != null;
	}

	@Override
	public Set<String> getScope() {
		if (scope != null) {
			Set<String> set =  Utils.delimitedStringToSet(scope, ",");
			set.remove("");
			return set;
		}
		return Collections.emptySet();
	}

	@Override
	public Set<String> getAuthorizedGrantTypes() {
		if (authorizedGrantTypes != null) {
			return Utils.delimitedStringToSet(authorizedGrantTypes, ",");
		}
		return Collections.emptySet();
	}

	@Override
	public Set<String> getRegisteredRedirectUri() {
		if (redirectUri != null) {
			return Utils.delimitedStringToSet(redirectUri, ",");
		}
		return Collections.emptySet();
	}
	
	public Set<String> getNativeAppSignaturesSet() {
		Map<String,Object> additionalInfo = getAdditionalInformation();
		if (additionalInfo != null && additionalInfo.containsKey(KEY_APP_SIGNATURES)) {
			return Utils.delimitedStringToSet(additionalInfo.get(KEY_APP_SIGNATURES).toString(), ",");
		}
		return Collections.emptySet();
	}
	
	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		if (authorities != null) {
			String[] arr = authorities.split(",");
			HashSet<GrantedAuthority> res = new HashSet<GrantedAuthority>();
			for (String s : arr) {
				res.add(new SimpleGrantedAuthority(s.trim()));
			}
			return res;
		}
		return Collections.emptySet();
	}

	@Override
	public Integer getAccessTokenValiditySeconds() {
		return accessTokenValidity;
	}

	@Override
	public Integer getRefreshTokenValiditySeconds() {
		return refreshTokenValidity;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getAdditionalInformation() {
		try {
			return mapper.readValue(additionalInformation, Map.class);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public boolean isAutoApprove(String scope) {
		return "true".equals(autoapprove) || (StringUtils.hasText(autoapprove) && StringUtils.commaDelimitedListToSet(autoapprove).contains(scope));
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

//	public String getMobileAppSchema() {
//		return mobileAppSchema;
//	}
//
//	public void setMobileAppSchema(String mobileAppSchema) {
//		this.mobileAppSchema = mobileAppSchema;
//	}
}
