package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.model.ClientApp;

/*
 * Client App serves UI and API
 */
public interface ClientAppService {
    /*
     * Client registration
     */
    public ClientApp getClient(String clientId);

    public ClientApp updateClient(String clientId, ClientApp app);

    public ClientApp registerClient(ClientApp app);

    public void deleteClient(String clientId);
}
