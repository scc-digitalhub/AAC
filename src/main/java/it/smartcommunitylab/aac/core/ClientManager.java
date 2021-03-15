package it.smartcommunitylab.aac.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.UserEntityService;

@Service
public class ClientManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // base services for users
    @Autowired
    private ClientEntityService clientService;

}
