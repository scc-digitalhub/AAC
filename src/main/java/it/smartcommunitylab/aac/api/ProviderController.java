package it.smartcommunitylab.aac.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.dto.ConfigurablePropertiesBean;
import it.smartcommunitylab.aac.dto.ProviderRegistrationBean;

@RestController
@RequestMapping("api")
public class ProviderController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProviderManager providerManager;

    /*
     * Identity providers
     * 
     * Manage only realm providers, with config stored
     */
    @GetMapping("/idptemplates/{realm}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public Collection<ProviderRegistrationBean> listTemplates(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {

        return providerManager.listProviderConfigurationTemplates(realm, ConfigurableProvider.TYPE_IDENTITY)
                .stream()
                .map(cp -> ProviderRegistrationBean.fromProvider(cp)).collect(Collectors.toList());
    }

    @GetMapping("/idp/{realm}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public Collection<ProviderRegistrationBean> listIdps(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {

        return providerManager.listProviders(realm, ConfigurableProvider.TYPE_IDENTITY)
                .stream()
                .map(cp -> {
                    ProviderRegistrationBean res = ProviderRegistrationBean.fromProvider(cp);
                    res.setRegistered(providerManager.isProviderRegistered(cp));
                    return res;
                }).collect(Collectors.toList());
    }

    @GetMapping("/idp/{realm}/{providerId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ProviderRegistrationBean getIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        ConfigurableProvider provider = providerManager.getProvider(realm, ConfigurableProvider.TYPE_IDENTITY,
                providerId);
        ProviderRegistrationBean res = ProviderRegistrationBean.fromProvider(provider);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        res.setRegistered(isRegistered);

        // if registered fetch active configuration
        if (isRegistered) {
            IdentityProvider idp = providerManager.getIdentityProvider(providerId);
            Map<String, Serializable> configMap = idp.getConfiguration().getConfiguration();
            // we replace config instead of merging, when active config can not be
            // modified anyway
            res.setConfiguration(configMap);
        }

        return res;
    }

    @PostMapping("/idp/{realm}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ProviderRegistrationBean addIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @Valid @RequestBody ProviderRegistrationBean registration) throws NoSuchRealmException {

        // unpack and build model
        String authority = registration.getAuthority();
        String type = registration.getType();
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        Map<String, Serializable> configuration = registration.getConfiguration();

        ConfigurableProvider provider = new ConfigurableProvider(authority, null, realm);
        provider.setName(name);
        provider.setDescription(description);
        provider.setType(type);
        provider.setEnabled(false);
        provider.setPersistence(persistence);
        provider.setConfiguration(configuration);

        provider = providerManager.addProvider(realm, provider);
        return ProviderRegistrationBean.fromProvider(provider);
    }

    @PutMapping("/idp/{realm}/{providerId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ProviderRegistrationBean updateIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @Valid @RequestBody ProviderRegistrationBean registration)
            throws NoSuchRealmException, NoSuchProviderException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);

        // we update only configuration
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        boolean enabled = (registration.getEnabled() != null ? registration.getEnabled().booleanValue() : false);
        Map<String, Serializable> configuration = registration.getConfiguration();

        provider.setName(name);
        provider.setDescription(description);
        provider.setPersistence(persistence);
        provider.setConfiguration(configuration);
        provider.setEnabled(enabled);

        provider = providerManager.updateProvider(realm, providerId, provider);
        ProviderRegistrationBean res = ProviderRegistrationBean.fromProvider(provider);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        res.setRegistered(isRegistered);

        return res;
    }

    @DeleteMapping("/idp/{realm}/{providerId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public void deleteIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {

        providerManager.deleteProvider(realm, providerId);

    }

    /*
     * Registration with authorities
     */

    @PutMapping("/idp/{realm}/{providerId}/status")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ProviderRegistrationBean registerIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @Valid @RequestBody ProviderRegistrationBean registration)
            throws NoSuchProviderException, NoSuchRealmException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);
        boolean enabled = (registration.getEnabled() != null ? registration.getEnabled().booleanValue() : true);

        if (enabled) {
            provider = providerManager.registerProvider(realm, providerId);
        } else {
            provider = providerManager.unregisterProvider(realm, providerId);
        }

        ProviderRegistrationBean res = ProviderRegistrationBean.fromProvider(provider);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        res.setRegistered(isRegistered);

        return res;

    }

}
