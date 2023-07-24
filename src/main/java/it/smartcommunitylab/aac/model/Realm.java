package it.smartcommunitylab.aac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.oauth.model.OAuth2ConfigurationMap;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

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

    public Realm() {
        this.oauthConfiguration = new OAuth2ConfigurationMap();
    }

    public Realm(String slug, String name) {
        this.name = name;
        this.slug = slug;
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

    public OAuth2ConfigurationMap getOAuthConfiguration() {
        return oauthConfiguration;
    }

    public void setOAuthConfiguration(OAuth2ConfigurationMap oauthConfiguration) {
        this.oauthConfiguration = oauthConfiguration;
    }
}
