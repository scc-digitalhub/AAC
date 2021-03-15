package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.base.BaseClient;

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

    /*
     * Client credentials
     */

    public Object getClientCredentials(String clientId) throws NoSuchClientException;

    public Object resetClientCredentials(String clientId) throws NoSuchClientException;

}
