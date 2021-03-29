package it.smartcommunitylab.aac.core.service;

import java.util.Collection;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.model.ClientApp;

/*
 * Client App serves UI and API
 */
public interface ClientAppService {
    /*
     * Client registration, per realm
     */
    public Collection<ClientApp> listClients(String realm);

    public ClientApp getClient(String realm, String clientId) throws NoSuchClientException;

    public ClientApp updateClient(String realm, String clientId, ClientApp app) throws NoSuchClientException;

    public ClientApp registerClient(String realm, String name);

    public ClientApp registerClient(String realm, ClientApp app);

    public void deleteClient(String realm, String clientId);

    /*
     * Configuration schema
     */
    public JsonSchema getConfigurationSchema();

}
