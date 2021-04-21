package it.smartcommunitylab.aac.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Client;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.services.ServicesManager;

@Service
public class RealmManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

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

    public Realm addRealm(Realm r) throws AlreadyRegisteredException {
        r.setSlug(r.getSlug().toLowerCase());
        return realmService.addRealm(r.getSlug(), r.getName());
    }

    public Realm updateRealm(String slug, Realm r) throws NoSuchRealmException {
        slug = slug.trim().toLowerCase();
        r.setSlug(slug);
        return realmService.updateRealm(slug, r.getName());
    }

    public Realm getRealm(String slug) throws NoSuchRealmException {
        return realmService.getRealm(slug);
    }

    public Collection<Realm> listRealms() {
        return realmService.listRealms();
    }

    public Collection<Realm> searchRealms(String keywords) {
        return realmService.searchRealms(keywords);
    }

    public Page<Realm> searchRealms(String keywords, Pageable pageRequest) {
        return realmService.searchRealms(keywords, pageRequest);
    }

    public void deleteRealm(String slug, boolean cleanup) throws NoSuchRealmException {
        Realm realm = realmService.getRealm(slug);

        if (cleanup) {
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
                        clientManager.deleteClient(clientId);
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
