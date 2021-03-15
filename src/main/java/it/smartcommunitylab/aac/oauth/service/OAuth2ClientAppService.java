package it.smartcommunitylab.aac.oauth.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.service.ClientAppService;
import it.smartcommunitylab.aac.core.service.ClientService;
import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;

/*
 * OAuth2 clients service
 */

@Service
public class OAuth2ClientAppService implements ClientService {

    /*
     * helpers
     */
    private ClientApp toApp(ClientEntity entity, OAuth2ClientEntity client) {
        ClientApp app = new ClientApp();
        app.setClientId(entity.getClientId());
        app.setType(SystemKeys.CLIENT_TYPE_OAUTH2);

        app.setName(entity.getName());
        app.setDescription(entity.getDescription());
        app.setRealm(entity.getRealm());

        app.setScopes(StringUtils.commaDelimitedListToSet(entity.getScopes()));
        app.setProviders(StringUtils.commaDelimitedListToSet(entity.getProviders()));

    }

}
