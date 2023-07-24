package it.smartcommunitylab.aac.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.smartcommunitylab.aac.SystemKeys;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.util.StringUtils;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurableIdentityProvider extends ConfigurableProvider {

    private Boolean linkable;
    private String persistence;
    private String events;
    private Integer position;

    @JsonIgnore
    private Map<String, String> hookFunctions = new HashMap<>();

    public ConfigurableIdentityProvider(String authority, String provider, String realm) {
        super(authority, provider, realm, SystemKeys.RESOURCE_IDENTITY);
        this.persistence = SystemKeys.PERSISTENCE_LEVEL_NONE;
        this.events = SystemKeys.EVENTS_LEVEL_DETAILS;
        this.linkable = true;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private ConfigurableIdentityProvider() {
        this((String) null, (String) null, (String) null);
    }

    public Boolean getLinkable() {
        return linkable;
    }

    public void setLinkable(Boolean linkable) {
        this.linkable = linkable;
    }

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    @JsonProperty("hookFunctions")
    public Map<String, String> getHookFunctionsBase64() {
        if (hookFunctions == null) {
            return null;
        }
        return hookFunctions
            .entrySet()
            .stream()
            .filter(e -> StringUtils.hasText(e.getValue()))
            .collect(
                Collectors.toMap(
                    e -> e.getKey(),
                    e -> {
                        return Base64.getEncoder().withoutPadding().encodeToString(e.getValue().getBytes());
                    }
                )
            );
    }

    @JsonProperty("hookFunctions")
    public void setHookFunctionsBase64(Map<String, String> hookFunctions) {
        if (hookFunctions != null) {
            this.hookFunctions =
                hookFunctions
                    .entrySet()
                    .stream()
                    .filter(e -> StringUtils.hasText(e.getValue()))
                    .collect(
                        Collectors.toMap(
                            e -> e.getKey(),
                            e -> {
                                return new String(Base64.getDecoder().decode(e.getValue().getBytes()));
                            }
                        )
                    );
        }
    }
}
