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

package it.smartcommunitylab.aac.oauth.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.templates.model.Language;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocalizationConfigurationMap implements ConfigurableProperties {

	private static ObjectMapper mapper = new ObjectMapper();
	private static final TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
	};

	private Set<Language> languages;

	public LocalizationConfigurationMap() {
	}

	public Set<Language> getLanguages() {
		return languages;
	}

	public void setLanguages(Set<Language> languages) {
		this.languages = languages;
	}

	@Override
	@JsonIgnore
	public Map<String, Serializable> getConfiguration() {
		// use mapper
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		return mapper.convertValue(this, typeRef);
	}

	@Override
	@JsonIgnore
	public void setConfiguration(Map<String, Serializable> props) {
		// use mapper
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		LocalizationConfigurationMap map = mapper.convertValue(props, LocalizationConfigurationMap.class);

		this.languages = map.getLanguages();
	}

}
