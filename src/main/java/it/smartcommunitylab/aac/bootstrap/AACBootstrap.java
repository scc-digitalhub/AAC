package it.smartcommunitylab.aac.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.ClientManager;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.UserManager;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.User;

@Component
public class AACBootstrap {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static ObjectMapper mapper = new ObjectMapper();

    @Value("${bootstrap.apply}")
    private boolean apply;

    @Value("${bootstrap.file}")
    private String source;

    @Value("${admin.username}")
    private String adminUsername;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

//    @Autowired
    private BootstrapConfig config;

    @Autowired
    private RealmManager realmManager;

    @Autowired
    private ProviderManager providerManager;

    @Autowired
    private ClientManager clientManager;

    @Autowired
    private UserManager userManager;

    // TODO rework with dedicated bootstrappers *per-manager*
    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            // base initalization
            logger.debug("application init");
            initServices();

            // custom bootstrap
            if (apply) {
                logger.debug("application bootstrap");
                bootstrap();
            } else {
                logger.debug("bootstrap disabled by config");
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void bootstrap() throws Exception {

        // read configuration
        Resource res = resourceLoader.getResource(source);
        if (!res.exists()) {
            logger.debug("no bootstrap file from " + source);
            return;
        }

        // read config
        config = yamlObjectMapper.readValue(res.getInputStream(), BootstrapConfig.class);
        
        /*
         * Realms creation
         */
        logger.debug("create bootstrap realms");

        // keep a cache of bootstrapped realms, we
        // will process only content related to these realms
        Map<String, Realm> realms = new HashMap<>();

        for (Realm r : config.getRealms()) {

            try {
                if (!StringUtils.hasText(r.getSlug())) {
                    // we ask id to be provided otherwise we create a new one every time
                    logger.error("error creating realm, missing slug");
                }

                logger.debug("create or update realm " + r.getSlug());

                Realm realm = realmManager.findRealm(r.getSlug());
                if (realm == null) {
                    realm = realmManager.addRealm(r);
                } else {
                    realm = realmManager.updateRealm(r.getSlug(), r);
                }

                // keep in cache
                realms.put(realm.getSlug(), realm);

            } catch (Exception e) {
                logger.error("error creating provider " + String.valueOf(r.getSlug()) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        /*
         * IdP
         */
        for (ConfigurableProvider cp : config.getProviders()) {

            try {
                if (!realms.containsKey(cp.getRealm())) {
                    // not managed here, skip
                    continue;
                }
                if (!StringUtils.hasText(cp.getProvider())) {
                    // we ask id to be provided otherwise we create a new one every time
                    logger.error("error creating provider, missing id");
                }
                // we support only idp for now
                if (SystemKeys.RESOURCE_IDENTITY.equals(cp.getType())) {
                    logger.debug("create or update provider " + cp.getProvider());
                    ConfigurableProvider provider = providerManager.findProvider(cp.getRealm(), cp.getProvider());

                    if (provider == null) {
                        provider = providerManager.addProvider(cp.getRealm(), cp);
                    } else {
                        provider = providerManager.unregisterProvider(cp.getRealm(), cp.getProvider());
                        provider = providerManager.updateProvider(cp.getRealm(), cp.getProvider(), cp);
                    }

                    if (cp.isEnabled()) {
                        // register
                        if (!providerManager.isProviderRegistered(provider)) {
                            provider = providerManager.registerProvider(provider.getRealm(), provider.getProvider());
                        }
                    }

                }

            } catch (Exception e) {
                logger.error("error creating provider " + String.valueOf(cp.getProvider()) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        /*
         * ClientApp
         */
        for (ClientApp ca : config.getClients()) {

            try {
                if (!StringUtils.hasText(ca.getRealm()) || !realms.containsKey(ca.getRealm())) {
                    // not managed here, skip
                    continue;
                }
                if (!StringUtils.hasText(ca.getClientId())) {
                    // we ask id to be provided otherwise we create a new one every time
                    logger.error("error creating client, missing clientId");
                }

                logger.debug("create or update client " + ca.getClientId());
                ClientApp client = clientManager.findClientApp(ca.getRealm(), ca.getClientId());

                if (client == null) {
                    client = clientManager.registerClientApp(ca.getRealm(), ca);
                } else {
                    client = clientManager.updateClientApp(ca.getRealm(), ca.getClientId(), ca);
                }

            } catch (Exception e) {
                logger.error("error creating client " + String.valueOf(ca.getClientId()) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // TODO users

        /*
         * Migrations?
         */

    }

    /*
     * Call init on each service we expect services to be independent and to execute
     * in their own transaction to avoid rollback issues across services
     */
    public void initServices() throws Exception {
//        /*
//         * Base user
//         */
//        logger.trace("init user");
//        userManager.init();
//
//        logger.trace("init registration");
//        registrationManager.init();
//
//        /*
//         * Base roles
//         */
//        logger.trace("init roles");
//        roleManager.init();
//
//        /*
//         * Base services
//         */
//        logger.trace("init services");
//        serviceManager.init();
//
//        /*
//         * Base clients
//         */
//        logger.trace("init client");
//        clientManager.init();

    }

//
//    public void executeMigrations() {
//
//    }

}
