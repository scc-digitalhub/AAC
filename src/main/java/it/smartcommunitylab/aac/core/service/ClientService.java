package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.base.BaseClient;
import it.smartcommunitylab.aac.core.model.ClientCredentials;
import java.util.Collection;

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

    public Collection<ClientCredentials> getClientCredentials(String clientId) throws NoSuchClientException;

    public ClientCredentials getClientCredentials(String clientId, String credentialsId) throws NoSuchClientException;

    public ClientCredentials resetClientCredentials(String clientId, String credentialsId) throws NoSuchClientException;

    public ClientCredentials setClientCredentials(String clientId, String credentialsId, ClientCredentials credentials)
        throws NoSuchClientException;

    public void removeClientCredentials(String clientId, String credentialsId) throws NoSuchClientException;
}
