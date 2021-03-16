package it.smartcommunitylab.aac.core.service;

import java.util.Collection;

import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.base.BaseClient;
import it.smartcommunitylab.aac.core.model.ClientCredentials;

/*
 * Client services
 * 
 * 
 */
public interface ClientService {

    /*
     * Client registration
     */
    public BaseClient getClient(String clientId) throws NoSuchClientException;

//    public Collection<BaseClient> listClients();

    /*
     * Client credentials
     */

    public ClientCredentials getClientCredentials(String clientId) throws NoSuchClientException;

    public ClientCredentials resetClientCredentials(String clientId) throws NoSuchClientException;

    public ClientCredentials setClientCredentials(String clientId, ClientCredentials credentials)
            throws NoSuchClientException;

}
