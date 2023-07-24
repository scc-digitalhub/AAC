package it.smartcommunitylab.aac.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import javax.validation.Valid;
import org.springframework.boot.context.properties.ConstructorBinding;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurableAccountProvider extends ConfigurableProvider {

    private String repositoryId;
    private String persistence;

    public ConfigurableAccountProvider(String authority, String provider, String realm) {
        super(authority, provider, realm, SystemKeys.RESOURCE_ACCOUNT);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private ConfigurableAccountProvider() {
        this((String) null, (String) null, (String) null);
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }
}
