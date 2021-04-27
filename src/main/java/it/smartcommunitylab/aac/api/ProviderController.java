package it.smartcommunitylab.aac.api;

import java.util.Collection;
import java.util.Map;

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
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.dto.ProviderRegistrationBean;
import it.smartcommunitylab.aac.model.ClientApp;

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
    public Collection<ConfigurableProvider> listTemplates(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {

        return providerManager.listProviderConfigurationTemplates(realm, ConfigurableProvider.TYPE_IDENTITY);
    }

    @GetMapping("/idp/{realm}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public Collection<ConfigurableProvider> listIdps(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {

        return providerManager.listProviders(realm, ConfigurableProvider.TYPE_IDENTITY);
    }

    @GetMapping("/idp/{realm}/{providerId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ConfigurableProvider getIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {

        return providerManager.getProvider(realm, ConfigurableProvider.TYPE_IDENTITY, providerId);
    }

    @PostMapping("/idp/{realm}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ConfigurableProvider addIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @Valid @RequestBody ProviderRegistrationBean registration) throws NoSuchRealmException {

        String authority = registration.getAuthority();
        String type = registration.getType();
        String name = registration.getName();
        Map<String, Object> configuration = registration.getConfiguration();

        ConfigurableProvider provider = providerManager.addProvider(realm, authority, type, name, configuration);
        return provider;
    }

    @PutMapping("/idp/{realm}/{providerId}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ConfigurableProvider updateIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @Valid @RequestBody ProviderRegistrationBean registration)
            throws NoSuchRealmException, NoSuchProviderException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);

        // we update only configuration
        String name = registration.getName();
        Map<String, Object> configuration = registration.getConfiguration();
        boolean enabled = registration.isEnabled();

        provider.setName(name);
        provider.setConfiguration(configuration);
        provider.setEnabled(enabled);

        return providerManager.updateProvider(realm, providerId, provider);
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
    public ConfigurableProvider registerIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @Valid @RequestBody ProviderRegistrationBean registration)
            throws NoSuchProviderException, NoSuchRealmException {

        boolean enabled = registration.isEnabled();

        if (enabled) {
            return providerManager.registerProvider(realm, providerId);
        } else {
            return providerManager.unregisterProvider(realm, providerId);
        }

    }

}
