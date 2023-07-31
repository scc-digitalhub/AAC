/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.model;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.oauth.model.OAuth2ConfigurationMap;
import it.smartcommunitylab.aac.oauth.model.TermsOfServiceConfigurationMap;

@Valid
@JsonInclude(Include.ALWAYS)
public class Realm {

	private String name;

	@NotBlank
	@Size(max = 128)
	private String slug;

	private boolean isEditable = true;
	private boolean isPublic = true;

	// TODO drop and move to provider
	private OAuth2ConfigurationMap oauthConfiguration;

	private TermsOfServiceConfigurationMap tosConfiguration;

	public Realm() {
		this.oauthConfiguration = new OAuth2ConfigurationMap();
	}

	public Realm(String slug, String name) {
		this.name = name;
		this.slug = slug;
		this.oauthConfiguration = new OAuth2ConfigurationMap();
		this.tosConfiguration = new TermsOfServiceConfigurationMap();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public boolean isEditable() {
		return isEditable;
	}

	public void setEditable(boolean isEditable) {
		this.isEditable = isEditable;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public OAuth2ConfigurationMap getOAuthConfiguration() {
		return oauthConfiguration;
	}

	public void setOAuthConfiguration(OAuth2ConfigurationMap oauthConfiguration) {
		this.oauthConfiguration = oauthConfiguration;
	}

	public TermsOfServiceConfigurationMap getTosConfiguration() {
		return tosConfiguration;
	}

	public void setTosConfiguration(TermsOfServiceConfigurationMap tosConfiguration) {
		this.tosConfiguration = tosConfiguration;
	}

}