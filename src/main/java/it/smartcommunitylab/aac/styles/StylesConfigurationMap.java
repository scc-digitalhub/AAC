package it.smartcommunitylab.aac.styles;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.core.model.ConfigurableProperties;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class StylesConfigurationMap implements ConfigurableProperties {

	private static ObjectMapper mapper = new ObjectMapper();
	private static final TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
	};

	private String customCss;
	private String logoFileId;

	public String getCustomCss() {
		return customCss;
	}

	public void setCustomCss(String customCss) {
		this.customCss = customCss;
	}

	public String getLogoFileId() {
		return logoFileId;
	}

	public void setLogoFileId(String logoFileId) {
		this.logoFileId = logoFileId;
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
		StylesConfigurationMap map = mapper.convertValue(props, StylesConfigurationMap.class);

		this.customCss = map.getCustomCss();
		this.logoFileId = map.getLogoFileId();
	}
}
