/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.config;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.claims.ClaimsService;
import it.smartcommunitylab.aac.claims.DefaultClaimsService;
import it.smartcommunitylab.aac.claims.ExtractorsRegistry;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwarePathUriBuilder;
import it.smartcommunitylab.aac.core.service.CoreUserTranslator;
import it.smartcommunitylab.aac.users.UserTranslator;
import it.smartcommunitylab.aac.users.service.UserService;
import java.io.IOException;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/*
 * AAC core config, all services should already be up and running now
 */
@Configuration
@Order(10)
public class AACConfig {

    @Value("${application.url}")
    private String applicationUrl;

    @Value("${bootstrap.file}")
    private String bootstrapFile;

    @Value("${bootstrap.apply}")
    private boolean bootstrapApply;

    @Autowired
    private ApplicationProperties appProps;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * Core aac should be bootstrapped before services, security etc
     */

    @Bean
    public BootstrapConfig bootstrapConfig() throws IOException {
        // manually load form yaml because spring properties
        // can't bind abstract classes via jsonTypeInfo
        // also a custom factory won't work because properties are exposed as strings.
        BootstrapConfig config = new BootstrapConfig();

        if (bootstrapApply && StringUtils.hasText(bootstrapFile)) {
            // read configuration
            Resource res = resourceLoader.getResource(bootstrapFile);
            if (!res.exists()) {
                throw new IllegalArgumentException("error loading bootstrap from " + bootstrapFile);
            }

            // read config
            config = yamlObjectMapper.readValue(res.getInputStream(), BootstrapConfig.class);
        }

        return config;
    }

    //    @Autowired
    //    private DataSource dataSource;

    //    @Autowired
    //    private AuthorityManager authorityManager;

    /*
     * provider manager depends on authorities + static config + datasource
     */

    //    @Autowired
    //    private ProviderManager providerManager;

    /*
     * authManager depends on provider + userService
     */
    //    @Autowired
    //    private SubjectService subjectService;
    //
    //    @Autowired
    //    private UserEntityService userService;
    //
    //    @Bean
    //    public ExtendedUserAuthenticationManager extendedAuthenticationManager() throws Exception {
    //        return new ExtendedUserAuthenticationManager(authorityManager, userService, subjectService);
    //    }
    //
    //    @Bean
    //    public AuthenticationHelper authenticationHelper() {
    //        return new DefaultSecurityContextAuthenticationHelper();
    //    }

    /*
     * initialize the execution service here and then build claims service
     */

    @Bean
    public ClaimsService claimsService(
        ExtractorsRegistry extractorsRegistry,
        ScriptExecutionService executionService,
        UserService userService
    ) {
        DefaultClaimsService service = new DefaultClaimsService(extractorsRegistry);
        service.setExecutionService(executionService);
        service.setUserService(userService);
        return service;
    }

    /*
     * Cross realm user translator
     */
    @Bean
    public UserTranslator userTranslator() {
        return new CoreUserTranslator();
    }

    /*
     * Entrypoint
     */
    // TODO make sure all filters use this bean to build urls..
    @Bean
    public RealmAwarePathUriBuilder realmUriBuilder() {
        return new RealmAwarePathUriBuilder(applicationUrl);
    }

    /*
     * Locale resolver
     */
    @Bean("localeResolver")
    public LocaleResolver localeResolver() {
        // use cookie resolver with default
        Assert.hasText(appProps.getLang(), "default app language must be set");
        Locale locale = StringUtils.parseLocale(appProps.getLang());
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setDefaultLocale(locale);
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }
    //
    //    /*
    //     * Thymeleaf engine with extensions
    //     */
    //    @Bean
    //    public SpringTemplateEngine templateEngine() {
    //        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    //        templateEngine.addDialect(new LayoutDialect());
    //        return templateEngine;
    //    }

    //    @Autowired
    //    private UserRepository userRepository;
    //
    ////    @Autowired
    ////    private ClientDetailsRepository clientDetailsRepository;
    //
    //    @Autowired
    //    private DataSource dataSource;

    //    @Autowired
    //    private TokenStore tokenStore;
    //
    //    @Autowired
    //    private OIDCTokenEnhancer tokenEnhancer;
    //
    //    @Autowired
    //    private AACJwtTokenConverter tokenConverter;
    //
    //    /*
    //     * OAuth
    //     */
    //    // TODO split specifics to dedicated configurer
    //
    //    @Bean
    //    public AutoJdbcTokenStore getTokenStore() throws PropertyVetoException {
    //        return new AutoJdbcTokenStore(dataSource);
    //    }
    //
    //    @Bean
    //    public JdbcApprovalStore getApprovalStore() throws PropertyVetoException {
    //        return new JdbcApprovalStore(dataSource);
    //    }
    //
    //    @Bean
    //    @Primary
    //    public JdbcClientDetailsService getClientDetails() throws PropertyVetoException {
    //        JdbcClientDetailsService bean = new AACJDBCClientDetailsService(dataSource);
    //        bean.setRowMapper(getClientDetailsRowMapper());
    //        return bean;
    //    }

    //    @Bean
    //    @Primary
    //    public UserDetailsService getInternalUserDetailsService() {
    //        return new InternalUserDetailsRepo();
    //    }
    //
    //    @Bean
    //    public ClientDetailsRowMapper getClientDetailsRowMapper() {
    //        return new ClientDetailsRowMapper(userRepository);
    //    }
    //
    //    @Bean
    //    @Primary
    //    public OAuth2ClientDetailsProvider getOAuth2ClientDetailsProvider() throws PropertyVetoException {
    //        return new OAuth2ClientDetailsProviderImpl(clientDetailsRepository);
    //    }

    //    @Bean
    //    public FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
    //        FilterRegistrationBean registration = new FilterRegistrationBean();
    //        registration.setFilter(filter);
    //        registration.setOrder(-100);
    //        return registration;
    //    }

    //    @Bean
    //    public CachedServiceScopeServices getResourceStorage() {
    //        return new CachedServiceScopeServices();
    //    }

    //    @Bean("appTokenServices")
    //    public NonRemovingTokenServices getTokenServices() throws PropertyVetoException {
    //        NonRemovingTokenServices bean = new NonRemovingTokenServices();
    //        bean.setTokenStore(tokenStore);
    //        bean.setSupportRefreshToken(true);
    //        bean.setReuseRefreshToken(true);
    //        bean.setAccessTokenValiditySeconds(accessTokenValidity);
    //        bean.setRefreshTokenValiditySeconds(refreshTokenValidity);
    //        bean.setClientDetailsService(getClientDetails());
    //        if (oauth2UseJwt) {
    //            bean.setTokenEnhancer(new AACTokenEnhancer(tokenEnhancer, tokenConverter));
    //        } else {
    //            bean.setTokenEnhancer(new AACTokenEnhancer(tokenEnhancer));
    //        }
    //
    //        return bean;
    //    }

    //    /*
    //     * Authorities handlers
    //     */
    //
    //    @Bean
    //    public IdentitySource getIdentitySource() {
    //        return new FileEmailIdentitySource();
    //    }
    //
    //    @Bean
    //    public AuthorityHandlerContainer getAuthorityHandlerContainer() {
    //        Map<String, AuthorityHandler> map = Maps.newTreeMap();
    //        map.put(Config.IDP_INTERNAL, getInternalHandler());
    //        FBAuthorityHandler fh = new FBAuthorityHandler();
    //        map.put("facebook", fh);
    //        AuthorityHandlerContainer bean = new AuthorityHandlerContainer(map);
    //        return bean;
    //    }
    //
    //    @Bean
    //    public DefaultAuthorityHandler getDefaultHandler() {
    //        return new DefaultAuthorityHandler();
    //    }
    //
    //    @Bean
    //    public InternalAuthorityHandler getInternalHandler() {
    //        return new InternalAuthorityHandler();
    //    }

}
