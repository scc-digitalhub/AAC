package it.smartcommunitylab.aac.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.oauth.model.OAuth2ConfigurationMap;

@Valid
@JsonInclude(Include.ALWAYS)
public class Realm {

    private String name;

    @NotBlank
    private String slug;

    private boolean isEditable = true;
    private boolean isPublic = true;

    private List<CustomizationBean> customization;

    private OAuth2ConfigurationMap oauthConfiguration;

    public Realm() {
        this.customization = Collections.emptyList();
        this.oauthConfiguration = new OAuth2ConfigurationMap();
    }

    public Realm(String slug, String name) {
        this.name = name;
        this.slug = slug;
        this.customization = Collections.emptyList();
        this.oauthConfiguration = new OAuth2ConfigurationMap();

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

    public List<CustomizationBean> getCustomization() {
        return customization;
    }

    public void setCustomization(List<CustomizationBean> customization) {
        this.customization = customization;
    }

    public CustomizationBean getCustomization(String key) {
        if (customization == null) {
            return null;
        }

        return customization.stream().filter(c -> c.getIdentifier().equals(key)).findFirst().orElse(null);
    }

    public OAuth2ConfigurationMap getOAuthConfiguration() {
        return oauthConfiguration;
    }

    public void setOAuthConfiguration(OAuth2ConfigurationMap oauthConfiguration) {
        this.oauthConfiguration = oauthConfiguration;
    }

}
