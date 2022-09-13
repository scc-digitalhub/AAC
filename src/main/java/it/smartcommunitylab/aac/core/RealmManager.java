package it.smartcommunitylab.aac.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import it.smartcommunitylab.aac.common.NoSuchGroupException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.Client;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.groups.GroupManager;
import it.smartcommunitylab.aac.model.Developer;
import it.smartcommunitylab.aac.model.Group;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.roles.RealmRoleManager;
import it.smartcommunitylab.aac.services.ServicesManager;

@Service
public class RealmManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final static Safelist WHITELIST_RELAXED_NOIMG = Config.WHITELIST_RELAXED_NOIMG;

    private int minLength = 3;

    @Autowired
    private RealmService realmService;

    @Autowired
    private ClientManager clientManager;

    @Autowired
    private UserManager userManager;

    @Autowired
    private IdentityProviderManager identityProviderManager;

    @Autowired
    private AttributeProviderManager attributeProviderManager;

    @Autowired
    private ServicesManager servicesManager;

    @Autowired
    private UserService userService;

    @Autowired
    protected AttributeSetsManager attributeManager;

    @Autowired
    protected GroupManager groupManager;

    @Autowired
    private RealmRoleManager roleManager;

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
    public Realm addRealm(@Valid @NotBlank Realm r) throws AlreadyRegisteredException {
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

        if (slug.length() < minLength) {
            throw new RegistrationException("slug min length is " + String.valueOf(minLength));
        }

        if (logger.isTraceEnabled()) {
            logger.trace("realm: " + slug + " name " + String.valueOf(name));
        }

        return realmService.addRealm(slug, name, r.isEditable(), r.isPublic());
    }

    @Transactional(readOnly = false)
    public Realm updateRealm(String slug, Realm r) throws NoSuchRealmException {
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

        // explode customization
        Map<String, Map<String, String>> customizationMap = null;
        if (r.getCustomization() != null) {
            customizationMap = new HashMap<>();

            for (CustomizationBean cb : r.getCustomization()) {

                String key = cb.getIdentifier();
                if (StringUtils.hasText(key) && cb.getResources() != null) {
                    Map<String, String> res = new HashMap<>();

                    // sanitize
                    for (Map.Entry<String, String> e : cb.getResources().entrySet()) {
                        String k = e.getKey();
                        String v = e.getValue();

                        if (StringUtils.hasText(k)) {
                            k = Jsoup.clean(k, Safelist.none());
                        }
                        if (StringUtils.hasText(v)) {
                            v = Jsoup.clean(v, WHITELIST_RELAXED_NOIMG);
                        }

                        if (StringUtils.hasText(k)) {
                            res.put(k, v);
                        }
                    }

                    customizationMap.put(key, res);
                }
            }
        }

        Map<String, Serializable> oauth2ConfigMap = null;
        if (r.getOAuthConfiguration() != null) {
            oauth2ConfigMap = r.getOAuthConfiguration().getConfiguration();
        }

        Realm realm = realmService.updateRealm(slug, name, r.isEditable(), r.isPublic(), oauth2ConfigMap,
                customizationMap);

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
            Collection<ConfigurableIdentityProvider> idps = identityProviderManager.listProviders(slug);
            for (ConfigurableIdentityProvider provider : idps) {
                try {
                    String providerId = provider.getProvider();
                    // stop provider, will terminate sessions
                    identityProviderManager.unregisterProvider(slug, providerId);

                    // remove provider
                    identityProviderManager.deleteProvider(slug, providerId);
                } catch (NoSuchProviderException | NoSuchAuthorityException | SystemException e) {
                    // skip
                    logger.error("Error deleting realm for provider {}: {}", provider.getProvider(), e.getMessage());
                }
            }

            // remove all attribute providers
            Collection<ConfigurableAttributeProvider> aps = attributeProviderManager.listProviders(slug);
            for (ConfigurableAttributeProvider provider : aps) {
                try {
                    String providerId = provider.getProvider();
                    // stop provider
                    attributeProviderManager.unregisterProvider(slug, providerId);

                    // remove provider
                    attributeProviderManager.deleteProvider(slug, providerId);
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

            // remove services
            List<it.smartcommunitylab.aac.services.Service> services = servicesManager.listServices(slug);
            for (it.smartcommunitylab.aac.services.Service service : services) {
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
            Collection<Group> groups = groupManager.getGroups(slug);
            for (Group group : groups) {
                try {
                    String groupId = group.getGroupId();

                    // remove, should cleanup user association for leftovers
                    groupManager.deleteGroup(slug, groupId);
                } catch (NoSuchGroupException e) {
                    // skip
                }
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
            String email) throws NoSuchRealmException, NoSuchUserException {
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
                user = userManager.inviteUser(SystemKeys.REALM_SYSTEM, email);
            } catch (NoSuchRealmException | NoSuchProviderException e) {
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
}
