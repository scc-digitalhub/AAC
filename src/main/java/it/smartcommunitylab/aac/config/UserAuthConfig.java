package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.core.ExtendedUserAuthenticationManager;
import it.smartcommunitylab.aac.core.service.AttributeProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(15)
public class UserAuthConfig {

    @Autowired
    private IdentityProviderAuthorityService identityProviderAuthorityService;

    @Autowired
    private AttributeProviderAuthorityService attributeProviderAuthorityService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private UserEntityService userService;

    @Bean
    public ExtendedUserAuthenticationManager extendedAuthenticationManager() throws Exception {
        return new ExtendedUserAuthenticationManager(
            identityProviderAuthorityService,
            attributeProviderAuthorityService,
            userService,
            subjectService
        );
    }
}
