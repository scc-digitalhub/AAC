package it.smartcommunitylab.aac.core.model;

import java.util.Collections;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.boot.context.properties.ConstructorBinding;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorBinding
public class ConfigurableTemplateProvider extends ConfigurableProvider {

    private Set<String> languages;

    public ConfigurableTemplateProvider(String authority, String provider, String realm) {
        super(authority, provider, realm, SystemKeys.RESOURCE_TEMPLATE);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private ConfigurableTemplateProvider() {
        this((String) null, (String) null, (String) null);
        this.languages = Collections.emptySet();
    }

    @Override
    public void setType(String type) {
        // not supported
    }

    public Set<String> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }

}
