package it.smartcommunitylab.aac.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AttributeSetsManager;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.config.ApplicationProperties;
import it.smartcommunitylab.aac.core.base.AbstractAccount;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.Client;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.TemplateProviderService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.dto.RealmConfig;
import it.smartcommunitylab.aac.groups.model.Group;
import it.smartcommunitylab.aac.groups.service.GroupService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Developer;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.roles.RealmRoleManager;
import it.smartcommunitylab.aac.roles.model.RealmRole;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.services.ServicesManager;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.service.TemplateService;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;

@Service
public class RealmManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static int SLUG_MIN_LENGTH = 3;

    @Autowired
    private ApplicationProperties appProps;

    @Autowired
    private RealmAwareUriBuilder uriBuilder;

    @Autowired
    private RealmService realmService;

    @Autowired
    private ClientManager clientManager;

    @Autowired
    private UserManager userManager;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private AttributeProviderService attributeProviderService;

    @Autowired
    private TemplateProviderService templateProviderService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ServicesManager servicesManager;

    @Autowired
    private UserService userService;

    @Autowired
    protected AttributeSetsManager attributeManager;

    @Autowired
    protected GroupService groupService;

    @Autowired
    private RealmRoleManager roleManager;

    @Autowired
    private UserAccountService<InternalUserAccount> internalUserAccountService;

    @Autowired
    private UserAccountService<OIDCUserAccount> oidcUserAccountService;

    @Autowired
    private UserAccountService<SamlUserAccount> samlUserAccountService;

    @Autowired
    private UserCredentialsService<WebAuthnUserCredential> webAuthnUserCredentialsService;

    @Autowired
    private UserCredentialsService<InternalUserPassword> internalUserPasswordService;

//    @Autowired
//    private SessionManager sessionManager;

    /*
     * Manage realms.
     * 
     * TODO add permissions!
     * 
     * TODO add role assignments, import/export etc
     */

    @Transactional(readOnly = false)
    public Realm addRealm(@Valid @NotBlank Realm r) throws RegistrationException {
        logger.debug("add realm {}", StringUtils.trimAllWhitespace(r.getSlug()));
        r.setSlug(r.getSlug().toLowerCase());

        // cleanup input
        String slug = r.getSlug();
        String name = r.getName();

        if (StringUtils.hasText(slug)) {
            slug = Jsoup.clean(slug, Safelist.none());
            slug = slug.trim().toLowerCase();
        }

        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
            name = name.trim();
        }

        if (!StringUtils.hasText(slug)) {
            throw new RegistrationException("slug cannot be empty");
        }

        if (!StringUtils.hasText(name)) {
            throw new RegistrationException("name cannot be empty");
        }

        if (slug.length() < SLUG_MIN_LENGTH) {
            throw new RegistrationException("slug min length is " + String.valueOf(SLUG_MIN_LENGTH));
        }

        if (logger.isTraceEnabled()) {
            logger.trace("realm: {}", r.toString());
        }

        return realmService.addRealm(slug, name, r.isEditable(), r.isPublic());
    }

    @Transactional(readOnly = false)
    public Realm updateRealm(String slug, Realm r) throws NoSuchRealmException, RegistrationException {
        logger.debug("update realm {}", StringUtils.trimAllWhitespace(slug));
        r.setSlug(slug);

        String name = r.getName();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
            name = name.trim();
        }
        if (!StringUtils.hasText(name)) {
            throw new RegistrationException("name cannot be empty");
        }

        Map<String, Serializable> oauth2ConfigMap = null;
        if (r.getOAuthConfiguration() != null) {
            oauth2ConfigMap = r.getOAuthConfiguration().getConfiguration();
        }

        Realm realm = realmService.updateRealm(slug, name, r.isEditable(), r.isPublic(), oauth2ConfigMap);

        return realm;
    }

    @Transactional(readOnly = true)
    public Realm findRealm(String slug) {
        logger.debug("find realm {}", StringUtils.trimAllWhitespace(slug));

        return realmService.findRealm(slug);
    }

    @Transactional(readOnly = true)
    public Realm getRealm(String slug) throws NoSuchRealmException {
        logger.debug("get realm {}", StringUtils.trimAllWhitespace(slug));

        return realmService.getRealm(slug);
    }

    @Transactional(readOnly = true)
    public Collection<Realm> listRealms() {
        logger.debug("list realms");

        return realmService.listRealms();
    }

    @Transactional(readOnly = true)
    public Collection<Realm> listRealms(boolean isPublic) {
        logger.debug("list realms with public {}", String.valueOf(isPublic));

        return realmService.listRealms(isPublic);
    }

    @Transactional(readOnly = true)
    public Collection<Realm> searchRealms(String keywords) {
        String query = StringUtils.trimAllWhitespace(keywords);
        logger.debug("search realms with query {}", String.valueOf(query));

        return realmService.searchRealms(query);
    }

    @Transactional(readOnly = true)
    public Page<Realm> searchRealms(String keywords, Pageable pageRequest) {
        String query = StringUtils.trimAllWhitespace(keywords);
        logger.debug("search realms with query {}", String.valueOf(query));

        return realmService.searchRealms(query, pageRequest);
    }

    @Transactional(readOnly = false)
    public void deleteRealm(String slug, boolean cleanup) throws NoSuchRealmException {
        logger.debug("delete realm {}", StringUtils.trimAllWhitespace(slug));
        Realm realm = realmService.getRealm(slug);

        if (realm != null && cleanup) {
            // remove all identity providers, will also invalidate sessions for idps
            Collection<ConfigurableIdentityProvider> idps = identityProviderService.listProviders(slug);
            for (ConfigurableIdentityProvider provider : idps) {
                try {
                    String providerId = provider.getProvider();
                    // stop provider, will terminate sessions
                    identityProviderService.unregisterProvider(providerId);

                    // remove provider
                    identityProviderService.deleteProvider(providerId);
                } catch (NoSuchProviderException | NoSuchAuthorityException | SystemException e) {
                    // skip
                    logger.error("Error deleting realm for provider {}: {}", provider.getProvider(), e.getMessage());
                }
            }

            // remove all attribute providers
            Collection<ConfigurableAttributeProvider> aps = attributeProviderService.listProviders(slug);
            for (ConfigurableAttributeProvider provider : aps) {
                try {
                    String providerId = provider.getProvider();
                    // stop provider
                    attributeProviderService.unregisterProvider(providerId);

                    // remove provider
                    attributeProviderService.deleteProvider(providerId);
                } catch (NoSuchProviderException | NoSuchAuthorityException | SystemException e) {
                    // skip
                    logger.error("Error deleting realm for provider {}: {}", provider.getProvider(), e.getMessage());
                }
            }

            // remove all template providers
            Collection<ConfigurableTemplateProvider> tps = templateProviderService.listProviders(slug);
            for (ConfigurableTemplateProvider provider : tps) {
                try {
                    String providerId = provider.getProvider();
                    // stop provider
                    templateProviderService.unregisterProvider(providerId);

                    // remove provider
                    templateProviderService.deleteProvider(providerId);
                } catch (NoSuchProviderException | NoSuchAuthorityException | SystemException e) {
                    // skip
                    logger.error("Error deleting realm for provider {}: {}", provider.getProvider(), e.getMessage());
                }
            }

            // remove clients
            List<Client> clients = clientManager.listClients(slug);
            for (Client client : clients) {
                try {
                    String clientId = client.getClientId();

                    // check ownership
                    if (client.getRealm().equals(slug)) {

                        // remove, will kill active sessions and cleanup
                        clientManager.deleteClientApp(slug, clientId);
                    }
                } catch (NoSuchClientException e) {
                    // skip
                }
            }

            // remove users
            List<User> users = userManager.listUsers(slug);
            for (User user : users) {
                try {
                    String subjectId = user.getSubjectId();

                    // remove, will kill active sessions and cleanup
                    // will also delete if this realm is owner
                    userManager.removeUser(slug, subjectId);

                } catch (NoSuchUserException e) {
                    // skip
                }
            }

            // remove all orphan credentials
            // TODO refactor using credentialsService
            List<String> passwords = internalUserPasswordService.findCredentialsByRealm(slug).stream()
                    .map(p -> p.getId()).collect(Collectors.toList());
            internalUserPasswordService.deleteAllCredentials(slug, passwords);

            List<String> credentials = webAuthnUserCredentialsService.findCredentialsByRealm(slug).stream()
                    .map(p -> p.getId()).collect(Collectors.toList());
            webAuthnUserCredentialsService.deleteAllCredentials(slug, credentials);

            // remove services
            List<it.smartcommunitylab.aac.services.model.ApiService> services = servicesManager.listServices(slug);
            for (it.smartcommunitylab.aac.services.model.ApiService service : services) {
                try {
                    String serviceId = service.getServiceId();

                    // remove, will cleanup
                    servicesManager.deleteService(slug, serviceId);

                } catch (NoSuchServiceException e) {
                    // skip
                }
            }

            // attributes
            Collection<AttributeSet> attributeSets = attributeManager.listAttributeSets(slug, false);
            for (AttributeSet set : attributeSets) {
                try {
                    String setId = set.getIdentifier();

                    // remove, should cleanup user association for leftovers
                    attributeManager.deleteAttributeSet(slug, setId);
                } catch (NoSuchAttributeSetException e) {
                    // skip
                }
            }

            // groups
            Collection<Group> groups = groupService.listGroups(slug);
            for (Group group : groups) {
                String groupId = group.getGroupId();

                // remove, should cleanup user association for leftovers
                groupService.deleteGroup(slug, groupId);
            }

            // roles
            Collection<RealmRole> roles = roleManager.getRealmRoles(slug);
            for (RealmRole role : roles) {
                try {
                    String roleId = role.getRoleId();

                    // remove, should cleanup user association for leftovers
                    roleManager.deleteRealmRole(slug, roleId);
                } catch (Exception e) {
                    // skip
                }
            }

            // templates
            Collection<TemplateModel> templates = templateService.listTemplatesByRealm(slug);
            for (TemplateModel template : templates) {
                try {
                    String templateId = template.getId();
                    templateService.deleteTemplate(templateId);
                } catch (Exception e) {
                    // skip
                }
            }
        }

        // remove realm
        realmService.deleteRealm(slug);

    }

    /*
     * Developers
     */
    public Collection<Developer> listDevelopers() {
        // hardcoded, all system users are global developers
        // TODO sanitize output with dedicated developer model
        return userService.listUsers(SystemKeys.REALM_SYSTEM).stream()
                .map(u -> toDeveloper(SystemKeys.REALM_SYSTEM, u))
                .collect(Collectors.toList());
    }

    public Collection<Developer> getDevelopers(String realm) throws NoSuchRealmException {
        Realm r = realmService.getRealm(realm);

        List<Developer> developers = userService.listUsersByAuthority(r.getSlug(), Config.R_DEVELOPER)
                .stream()
                .map(u -> toDeveloper(realm, u))
                .collect(Collectors.toList());

        List<Developer> admins = userService.listUsersByAuthority(r.getSlug(), Config.R_ADMIN)
                .stream()
                .map(u -> toDeveloper(realm, u))
                .collect(Collectors.toList());

        return Stream.concat(developers.stream(), admins.stream()).collect(Collectors.toSet());
    }

    public Developer updateDeveloper(String realm, String subjectId, Collection<String> roles)
            throws NoSuchRealmException, NoSuchUserException {

        Set<String> devRoles = roles.stream().filter(r -> Config.R_ADMIN.equals(r) || Config.R_DEVELOPER.equals(r))
                .collect(Collectors.toSet());

        Realm r = realmService.getRealm(realm);
        userService.setUserAuthorities(subjectId, r.getSlug(), devRoles);

        return toDeveloper(realm, userService.getUser(subjectId, realm));
    }

    public void removeDeveloper(String realm, String subjectId) throws NoSuchRealmException, NoSuchUserException {
        Realm r = realmService.getRealm(realm);
        userService.setUserAuthorities(subjectId, r.getSlug(), null);
    }

    private Developer toDeveloper(String realm, User user) {
        Developer dev = new Developer(user.getSubjectId(), realm);

        dev.setUsername(user.getUsername());
        dev.setEmail(user.getEmail());

        dev.setAuthorities(user.getAuthorities());

        return dev;
    }

    public Developer inviteDeveloper(String realm, String subjectId,
            String email) throws NoSuchRealmException, NoSuchUserException, RegistrationException {
        User user = null;
        if (StringUtils.hasText(subjectId)) {
            // lookup by subject global
            user = userService.findUser(subjectId);
        }
        if (user == null && StringUtils.hasText(email)) {
            // lookup by email in system
            user = userService.findUsersByEmailAddress(SystemKeys.REALM_SYSTEM, email).stream().findFirst()
                    .orElse(null);
        }

        if (user == null && StringUtils.hasText(email)) {
            // invite in sys realm by email
            try {
                user = userManager.inviteUser(SystemKeys.REALM_SYSTEM, SystemKeys.AUTHORITY_INTERNAL, email);
            } catch (NoSuchRealmException | NoSuchProviderException | NoSuchAuthorityException e) {
                // nothing we can do, registration is unavailable
            }
        }

        if (user == null) {
            // error
            throw new IllegalArgumentException("user must already exists or provide a valid email");
        }

        // assign developer role
        return updateDeveloper(realm, user.getSubjectId(), Collections.singleton(Config.R_DEVELOPER));

    }

    public ApplicationProperties getRealmProps(String realm) throws NoSuchRealmException {
        // load realm
        Realm r = realmService.getRealm(realm);

        ApplicationProperties props = new ApplicationProperties();
        props.setName(r.getName());

        // build props from global
        // TODO add fields to realm config
        props.setEmail(appProps.getEmail());
        props.setLang(appProps.getLang());
        props.setLogo(appProps.getLogo());

        // via urlBuilder
        String url = uriBuilder.buildUrl(realm, "/");
        props.setUrl(url);

        return props;
    }

    /*
     * Realm full config
     */

    public RealmConfig getRealmConfig(String realm) throws NoSuchRealmException {
        // load realm
        Realm r = realmService.getRealm(realm);

        // build config
        RealmConfig rc = new RealmConfig(r);

        // providers
        Collection<ConfigurableIdentityProvider> idps = identityProviderService.listProviders(realm);
        Collection<ConfigurableAttributeProvider> aps = attributeProviderService.listProviders(realm);
        Collection<ConfigurableTemplateProvider> tps = templateProviderService.listProviders(realm);

        rc.setIdentityProviders(new ArrayList<>(idps));
        rc.setAttributeProviders(new ArrayList<>(aps));

        if (!tps.isEmpty()) {
            // pick first as config
            // TODO refactor
            rc.setTemplates(tps.iterator().next());
        }

        // services
        List<it.smartcommunitylab.aac.services.model.ApiService> services = servicesManager.listServices(realm);
        rc.setServices(services);

        // clients
        Collection<ClientApp> apps = clientManager.listClientApps(realm);
        rc.setClientApps(new ArrayList<>(apps));

        // user accounts
        List<InternalUserAccount> internalUsers = internalUserAccountService.findAccountByRealm(realm);
        List<OIDCUserAccount> oidcUsers = oidcUserAccountService.findAccountByRealm(realm);
        List<SamlUserAccount> samlUsers = samlUserAccountService.findAccountByRealm(realm);

        List<AbstractAccount> users = Stream.of(internalUsers, oidcUsers, samlUsers)
                .flatMap(l -> l.stream())
                .collect(Collectors.toList());

        rc.setUsers(users);
        // credentials
        // TODO

        return rc;
    }
}
