package it.smartcommunitylab.aac.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Client;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.services.ServicesManager;

@Service
public class RealmManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private int minLength = 3;

    @Autowired
    private RealmService realmService;

    @Autowired
    private ClientManager clientManager;

    @Autowired
    private UserManager userManager;

    @Autowired
    private ProviderManager providerManager;

    @Autowired
    private ServicesManager servicesManager;

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
    public Realm addRealm(Realm r) throws AlreadyRegisteredException {
        logger.debug("add realm");
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
        logger.debug("update realm " + String.valueOf(slug));

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
                            v = Jsoup.clean(v, Safelist.none());
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
        logger.debug("find realm " + String.valueOf(slug));

        return realmService.findRealm(slug);
    }

    @Transactional(readOnly = true)
    public Realm getRealm(String slug) throws NoSuchRealmException {
        logger.debug("get realm " + String.valueOf(slug));

        return realmService.getRealm(slug);
    }

    @Transactional(readOnly = true)
    public Collection<Realm> listRealms() {
        logger.debug("list realms");

        return realmService.listRealms();
    }

    @Transactional(readOnly = true)
    public Collection<Realm> listRealms(boolean isPublic) {
        logger.debug("list realms with public " + String.valueOf(isPublic));

        return realmService.listRealms(isPublic);
    }

    @Transactional(readOnly = true)
    public Collection<Realm> searchRealms(String keywords) {
        logger.debug("search realms with query " + String.valueOf(keywords));

        return realmService.searchRealms(keywords);
    }

    @Transactional(readOnly = true)
    public Page<Realm> searchRealms(String keywords, Pageable pageRequest) {
        logger.debug("search realms with query " + String.valueOf(keywords));

        return realmService.searchRealms(keywords, pageRequest);
    }

    @Transactional(readOnly = false)
    public void deleteRealm(String slug, boolean cleanup) throws NoSuchRealmException {
        logger.debug("delete realm " + String.valueOf(slug));
        Realm realm = realmService.getRealm(slug);

        if (realm != null && cleanup) {
            // remove all providers, will also invalidate sessions for idps
            Collection<ConfigurableProvider> providers = providerManager.listProviders(slug);
            for (ConfigurableProvider provider : providers) {
                try {
                    String providerId = provider.getProvider();

                    // check ownership
                    if (provider.getRealm().equals(slug)) {

                        // stop provider, will terminate sessions
                        providerManager.unregisterProvider(slug, providerId);

                        // remove provider
                        providerManager.deleteProvider(slug, providerId);
                    }
                } catch (NoSuchProviderException e) {
                    // skip
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

            // TODO attributes
        }

        // remove realm
        realmService.deleteRealm(slug);

    }

}
