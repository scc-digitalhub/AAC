package it.smartcommunitylab.aac.core;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.model.Client;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;

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

//    @Autowired
//    private SessionManager sessionManager;

    /*
     * Manage realms.
     * 
     * TODO add permissions!
     * 
     * TODO add role assignments, import/export etc
     */

    public Realm addRealm(String slug, String name) throws AlreadyRegisteredException {
        return realmService.addRealm(slug, name);
    }

    public Realm updateRealm(String slug, String name) throws NoSuchRealmException {
        return realmService.updateRealm(slug, name);
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

    public void deleteRealm(String slug, String cleanup) throws NoSuchRealmException {
        Realm realm = realmService.getRealm(slug);

        // remove identity providers, will also invalidate sessions
        Collection<IdentityProvider> idps = providerManager.getIdentityProviders(slug);
        for (IdentityProvider idp : idps) {
            try {
                String providerId = idp.getProvider();

                // check ownership
                if (idp.getRealm().equals(slug)) {

                    // stop provider, will terminate sessions
                    providerManager.unregisterProvider(providerId);

                    // remove provider
                    providerManager.deleteProvider(providerId);
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

                // check ownership
                if (user.getSource().equals(slug)) {
                    // remove, will kill active sessions and cleanup
                    userManager.deleteUser(subjectId);
                }
            } catch (NoSuchUserException e) {
                // skip
            }
        }

        // TODO services

        // TODO attributes

        // remove realm
        realmService.deleteRealm(slug);

    }

}
