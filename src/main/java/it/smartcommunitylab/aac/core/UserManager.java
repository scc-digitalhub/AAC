package it.smartcommunitylab.aac.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.service.AttributeEntityService;
import it.smartcommunitylab.aac.core.service.RoleService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.InternalUserManager;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.openid.OIDCUserManager;

@Service
public class UserManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // base services for users
    @Autowired
    private UserEntityService userService;

    @Autowired
    private AttributeEntityService attributeService;

    @Autowired
    private RoleService roleService;

    // hardcoded configuration for authorities
    // TODO move to dynamic list with interface, builders (+ injectors?)
    // identity + attribute providers

    @Autowired
    private InternalUserManager internalUserManager;

    @Autowired
    private OIDCUserManager oidcUserManager;


}
