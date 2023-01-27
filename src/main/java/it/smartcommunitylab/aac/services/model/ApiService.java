package it.smartcommunitylab.aac.services.model;

import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.base.AbstractApiResource;

/*
 * A service defines an api composed of a namespace (used as audience)
 */

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiService extends AbstractApiResource<ApiServiceScope> {

    @Size(max = 128)
    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String serviceId;

    @NotBlank
    @Pattern(regexp = SystemKeys.NAMESPACE_PATTERN)
    private String resource;

    @NotBlank
    @Pattern(regexp = SystemKeys.NAMESPACE_PATTERN)
    private String namespace;

    private Collection<ApiServiceScope> scopes = Collections.emptyList();
    private Collection<ApiServiceClaimDefinition> claims = Collections.emptyList();

    /*
     * Claim extractor functions
     */
    @JsonIgnore
    private String userClaimsExtractor;

    @JsonIgnore
    private String clientClaimsExtractor;

    /*
     * Claim webhook
     */
    private String webhookExtractor;

    public ApiService() {
        super(SystemKeys.AUTHORITY_SERVICE, (String) null);
    }

    public ApiService(String serviceId) {
        super(SystemKeys.AUTHORITY_SERVICE, serviceId);
    }

    @Override
    public String getResourceId() {
        return serviceId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Collection<ApiServiceScope> getScopes() {
        return scopes;
    }

    public void setScopes(Collection<ApiServiceScope> scopes) {
        this.scopes = scopes;
    }

    @Override
    public Collection<ApiServiceClaimDefinition> getClaims() {
        return claims;
    }

    public void setClaims(Collection<ApiServiceClaimDefinition> claims) {
        this.claims = claims;
    }

    public String getWebhookExtractor() {
        return webhookExtractor;
    }

    public void setWebhookExtractor(String webhookExtractor) {
        this.webhookExtractor = webhookExtractor;
    }

    public String getUserClaimsExtractor() {
        return userClaimsExtractor;
    }

    public void setUserClaimsExtractor(String userClaimsExtractor) {
        this.userClaimsExtractor = userClaimsExtractor;
    }

    public String getClientClaimsExtractor() {
        return clientClaimsExtractor;
    }

    public void setClientClaimsExtractor(String clientClaimsExtractor) {
        this.clientClaimsExtractor = clientClaimsExtractor;
    }

    @JsonProperty("userClaimsExtractor")
    public String getUserClaimsExtractorBase64() {
        if (userClaimsExtractor == null) {
            return null;
        }

        return Base64.getEncoder().encodeToString(userClaimsExtractor.getBytes());
    }

    @JsonProperty("userClaimsExtractor")
    public void setUserClaimsExtractorBase64(String function) {
        if (function != null) {
            this.userClaimsExtractor = new String(Base64.getDecoder().decode(function.getBytes()));
        }
    }

    @JsonProperty("clientClaimsExtractor")
    public String getClientClaimsExtractorBase64() {
        if (clientClaimsExtractor == null) {
            return null;
        }

        return Base64.getEncoder().encodeToString(clientClaimsExtractor.getBytes());
    }

    @JsonProperty("clientClaimsExtractor")
    public void setClientClaimsExtractorBase64(String function) {
        if (function != null) {
            this.clientClaimsExtractor = new String(Base64.getDecoder().decode(function.getBytes()));
        }
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_SERVICE;
    }

    @Override
    public String getProvider() {
        return serviceId;
    }

    @Override
    public String getId() {
        return serviceId;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_API_RESOURCE;
    }

}
