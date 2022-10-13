package it.smartcommunitylab.aac.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.services.Service;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RealmConfig {

    // realm
    @JsonUnwrapped
    @NotNull
    private Realm realm;

    // providers config
    private List<ConfigurableIdentityProvider> identityProviders;
    private List<ConfigurableAttributeProvider> attributeProviders;
    private ConfigurableTemplateProvider templates;

    // services
    private List<Service> services;

    // clientApps
    private List<ClientApp> clientApps;

    // user accounts
    private List<InternalUserAccount> internalUsers;
    private List<OIDCUserAccount> oidcUsers;
    private List<SamlUserAccount> samlUsers;

    public Realm getRealm() {
        return realm;
    }

    public void setRealm(Realm realm) {
        this.realm = realm;
    }

    public List<ConfigurableIdentityProvider> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<ConfigurableIdentityProvider> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public List<ConfigurableAttributeProvider> getAttributeProviders() {
        return attributeProviders;
    }

    public void setAttributeProviders(List<ConfigurableAttributeProvider> attributeProviders) {
        this.attributeProviders = attributeProviders;
    }

    public ConfigurableTemplateProvider getTemplates() {
        return templates;
    }

    public void setTemplates(ConfigurableTemplateProvider templates) {
        this.templates = templates;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public List<ClientApp> getClientApps() {
        return clientApps;
    }

    public void setClientApps(List<ClientApp> clientApps) {
        this.clientApps = clientApps;
    }

    public List<InternalUserAccount> getInternalUsers() {
        return internalUsers;
    }

    public void setInternalUsers(List<InternalUserAccount> internalUsers) {
        this.internalUsers = internalUsers;
    }

    public List<OIDCUserAccount> getOidcUsers() {
        return oidcUsers;
    }

    public void setOidcUsers(List<OIDCUserAccount> oidcUsers) {
        this.oidcUsers = oidcUsers;
    }

    public List<SamlUserAccount> getSamlUsers() {
        return samlUsers;
    }

    public void setSamlUsers(List<SamlUserAccount> samlUsers) {
        this.samlUsers = samlUsers;
    }

}
