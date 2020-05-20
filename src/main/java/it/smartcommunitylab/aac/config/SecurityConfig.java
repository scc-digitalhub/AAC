/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/
package it.smartcommunitylab.aac.config;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.Filter;
import javax.sql.DataSource;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.CompositeTokenGranter;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.approval.JdbcApprovalStore;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLBootstrap;
import org.springframework.security.saml.SAMLDiscovery;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.SAMLLogoutFilter;
import org.springframework.security.saml.SAMLLogoutProcessingFilter;
import org.springframework.security.saml.SAMLProcessingFilter;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.metadata.CachingMetadataManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.security.saml.metadata.MetadataMemoryProvider;
import org.springframework.security.saml.parser.ParserPoolHolder;
import org.springframework.security.saml.processor.HTTPArtifactBinding;
import org.springframework.security.saml.processor.HTTPPAOS11Binding;
import org.springframework.security.saml.processor.HTTPPostBinding;
import org.springframework.security.saml.processor.HTTPRedirectDeflateBinding;
import org.springframework.security.saml.processor.HTTPSOAP11Binding;
import org.springframework.security.saml.processor.SAMLBinding;
import org.springframework.security.saml.processor.SAMLProcessorImpl;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.ArtifactResolutionProfile;
import org.springframework.security.saml.websso.ArtifactResolutionProfileImpl;
import org.springframework.security.saml.websso.SingleLogoutProfile;
import org.springframework.security.saml.websso.SingleLogoutProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfile;
import org.springframework.security.saml.websso.WebSSOProfileConsumer;
import org.springframework.security.saml.websso.WebSSOProfileConsumerHoKImpl;
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl;
import org.springframework.security.saml.websso.WebSSOProfileECPImpl;
import org.springframework.security.saml.websso.WebSSOProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfileOptions;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.ui.velocity.VelocityEngineFactoryBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.filter.CorsFilter;
import org.yaml.snakeyaml.Yaml;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.auth.cie.FileEmailIdentitySource;
import it.smartcommunitylab.aac.auth.cie.IdentitySource;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.manager.OAuth2ClientDetailsProviderImpl;
import it.smartcommunitylab.aac.manager.ProviderServiceAdapter;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsRowMapper;
import it.smartcommunitylab.aac.model.MockDataMappings;
import it.smartcommunitylab.aac.oauth.AACJDBCClientDetailsService;
import it.smartcommunitylab.aac.oauth.AACJwtTokenConverter;
import it.smartcommunitylab.aac.oauth.AACOAuth2RequestFactory;
import it.smartcommunitylab.aac.oauth.AACOAuth2RequestValidator;
import it.smartcommunitylab.aac.oauth.AACRememberMeServices;
import it.smartcommunitylab.aac.oauth.AACTokenEnhancer;
import it.smartcommunitylab.aac.oauth.AACWebResponseExceptionTranslator;
import it.smartcommunitylab.aac.oauth.AutoJdbcAuthorizationCodeServices;
import it.smartcommunitylab.aac.oauth.AutoJdbcTokenStore;
import it.smartcommunitylab.aac.oauth.ClientCredentialsRegistrationFilter;
import it.smartcommunitylab.aac.oauth.ClientCredentialsTokenEndpointFilter;
import it.smartcommunitylab.aac.oauth.ContextExtender;
import it.smartcommunitylab.aac.oauth.InternalPasswordEncoder;
import it.smartcommunitylab.aac.oauth.InternalUserDetailsRepo;
import it.smartcommunitylab.aac.oauth.MockDataAwareOAuth2SuccessHandler;
import it.smartcommunitylab.aac.oauth.MultitenantOAuth2ClientAuthenticationProcessingFilter;
import it.smartcommunitylab.aac.oauth.NativeTokenGranter;
import it.smartcommunitylab.aac.oauth.NonRemovingTokenServices;
import it.smartcommunitylab.aac.oauth.OAuth2ClientDetailsProvider;
import it.smartcommunitylab.aac.oauth.OAuthClientUserDetails;
import it.smartcommunitylab.aac.oauth.OAuthFlowExtensions;
import it.smartcommunitylab.aac.oauth.OAuthProviders;
import it.smartcommunitylab.aac.oauth.OAuthProviders.ClientResources;
import it.smartcommunitylab.aac.oauth.token.PKCEAwareTokenGranter;
import it.smartcommunitylab.aac.oauth.token.RefreshTokenGranter;
import it.smartcommunitylab.aac.oauth.token.ResourceOwnerPasswordTokenGranter;
import it.smartcommunitylab.aac.oauth.UserApprovalHandler;
import it.smartcommunitylab.aac.oauth.UserDetailsRepo;
import it.smartcommunitylab.aac.oauth.WebhookOAuthFlowExtensions;
import it.smartcommunitylab.aac.oauth.endpoint.TokenIntrospectionEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenRevocationEndpoint;
import it.smartcommunitylab.aac.oauth.token.AuthorizationCodeTokenGranter;
import it.smartcommunitylab.aac.oauth.token.ClientCredentialsTokenGranter;
import it.smartcommunitylab.aac.oauth.token.ImplicitTokenGranter;
import it.smartcommunitylab.aac.openid.endpoint.UserInfoEndpoint;
import it.smartcommunitylab.aac.openid.service.OIDCTokenEnhancer;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;
import it.smartcommunitylab.aac.saml.AACSAMLBootstrap;
import it.smartcommunitylab.aac.saml.AACSAMLUserDetailsService;
import it.smartcommunitylab.aac.saml.SamlLoginSuccessHandler;
import it.smartcommunitylab.aac.saml.adp.ADPWebSSOProfileConsumer;
import it.smartcommunitylab.aac.saml.spid.SPIDMetadataGenerator;
import it.smartcommunitylab.aac.saml.spid.SPIDWebSSOProfile;

@Configuration
@EnableOAuth2Client
@EnableConfigurationProperties
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("classpath:/testdata.yml")
	private Resource dataMapping;

	@Value("${application.url}")
	private String applicationURL;

	@Value("${security.restricted}")
	private boolean restrictedAccess;

	@Value("${security.rememberme.key}")
	private String remembermeKey;
	
    @Value("${oauth2.jwt}")
    private boolean oauth2UseJwt;

    @Value("${security.accesstoken.validity}")
    private int accessTokenValidity;
    
    @Value("${security.refreshtoken.validity}")
    private int refreshTokenValidity;    
    
    @Value("${security.authcode.validity}")
    private int authCodeValidity;   
       
    @Value("${oauth2.pkce.allowRefresh}")
    private boolean oauth2PKCEAllowRefresh;        

    @Value("${oauth2.clientCredentials.allowRefresh}")
    private boolean oauth2ClientCredentialsAllowRefresh;          

    @Value("${oauth2.resourceOwnerPassword.allowRefresh}")
    private boolean oauth2ResourceOwnerPasswordAllowRefresh;          
    
    @Value("${security.redirects.matchports}")
    private boolean configMatchPorts;

    @Value("${security.redirects.matchsubdomains}")
    private boolean configMatchSubDomains;
    
    
    @Value("${saml.type}")
    private String samlType;
        
    @Value("${saml.sp.name}")
    private String samlSpName;
    
    @Value("${saml.sp.description}")
    private String samlSpDescription;
    
    @Value("${saml.sp.id}")
    private String samlSpEntityId;
    
    @Value("${saml.idp.metadata}")
    private String samlIdpMetadata;
    
   
	@Autowired
	OAuth2ClientContext oauth2ClientContext;

	@Autowired
	private ClientDetailsRepository clientDetailsRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private OIDCTokenEnhancer tokenEnhancer;	
	
	@Autowired
	private AACJwtTokenConverter tokenConverter;
	
	@Autowired
	private TokenStore tokenStore;

	@Bean
	public AutoJdbcTokenStore getTokenStore() throws PropertyVetoException {
		return new AutoJdbcTokenStore(dataSource);
	}

	@Bean
	public JdbcApprovalStore getApprovalStore() throws PropertyVetoException {
		return new JdbcApprovalStore(dataSource);
	}

	@Bean
	public JdbcClientDetailsService getClientDetails() throws PropertyVetoException {
		JdbcClientDetailsService bean = new AACJDBCClientDetailsService(dataSource);
		bean.setRowMapper(getClientDetailsRowMapper());
		return bean;
	}

	@Bean 
	public PersistentTokenBasedRememberMeServices rememberMeServices() {
		AACRememberMeServices service = new AACRememberMeServices(remembermeKey, new UserDetailsRepo(userRepository), persistentTokenRepository());
        service.setCookieName(Config.COOKIE_REMEMBER_ME);
        service.setParameter(Config.PARAM_REMEMBER_ME);
        service.setTokenValiditySeconds(3600 * 24 * 60); // two month
		return service;
	}
	
	@Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepositoryImpl = new JdbcTokenRepositoryImpl();
        tokenRepositoryImpl.setDataSource(dataSource);
        return tokenRepositoryImpl;
    }
	
	@Bean
	public UserDetailsService getInternalUserDetailsService() {
		return new InternalUserDetailsRepo();
	}

	@Bean
	public ClientDetailsRowMapper getClientDetailsRowMapper() {
		return new ClientDetailsRowMapper(userRepository);
	}
	
	@Bean
	public OAuth2ClientDetailsProvider getOAuth2ClientDetailsProvider() throws PropertyVetoException {
		return new OAuth2ClientDetailsProviderImpl(clientDetailsRepository);
	}


	@Bean
	public FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(filter);
		registration.setOrder(-100);
		return registration;
	}

	@Bean
	public FilterRegistrationBean corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.addAllowedOrigin("*");
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		source.registerCorsConfiguration("/**", config);
		FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
		bean.setOrder(0);
		return bean;
	}

	@Bean
	@ConfigurationProperties("oauth-providers")
	public OAuthProviders oauthProviders() {
		return new OAuthProviders();
	}

	@Bean
	public InternalPasswordEncoder getInternalPasswordEncoder() {
		return new InternalPasswordEncoder();
	}

	@Bean
	public IdentitySource getIdentitySource() {
		return new FileEmailIdentitySource();
	}

	@Bean("appTokenServices")
	public NonRemovingTokenServices getTokenServices() throws PropertyVetoException {
		NonRemovingTokenServices bean = new NonRemovingTokenServices();
		bean.setTokenStore(tokenStore);
		bean.setSupportRefreshToken(true);
		bean.setReuseRefreshToken(true);
		bean.setAccessTokenValiditySeconds(accessTokenValidity);
		bean.setRefreshTokenValiditySeconds(refreshTokenValidity);
		bean.setClientDetailsService(getClientDetails());
		if (oauth2UseJwt) {
		    bean.setTokenEnhancer(new AACTokenEnhancer(tokenEnhancer, tokenConverter));
		} else {
	        bean.setTokenEnhancer(new AACTokenEnhancer(tokenEnhancer));		    
		}

		return bean;
	}
	
	private Filter extOAuth2Filter() throws Exception {
		CompositeFilter filter = new CompositeFilter();
		List<Filter> filters = new ArrayList<>();
		List<ClientResources> providers = oauthProviders().getProviders();
		for (ClientResources client : providers) {
			String id = client.getProvider();
			filters.add(extOAuth2Filter(client, Utils.filterRedirectURL(id), "/eauth/" + id));
		}
		filter.setFilters(filters);
		return filter;

	}

	private Filter extOAuth2Filter(ClientResources client, String path, String target) throws Exception {
		OAuth2ClientAuthenticationProcessingFilter filter = new MultitenantOAuth2ClientAuthenticationProcessingFilter(client.getProvider(), path, getOAuth2ClientDetailsProvider());

		Yaml yaml = new Yaml();
		MockDataMappings data = yaml.loadAs(dataMapping.getInputStream(), MockDataMappings.class);

		filter.setAuthenticationSuccessHandler(new MockDataAwareOAuth2SuccessHandler(target, data));

		OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(), oauth2ClientContext);
		filter.setRestTemplate(template);
		UserInfoTokenServices tokenServices = new UserInfoTokenServices(client.getResource().getUserInfoUri(),
				client.getClient().getClientId());
		tokenServices.setRestTemplate(template);
		filter.setTokenServices(tokenServices);
		return filter;
	}

    /*
     * SAML
     */
	@Autowired
	private KeyManager samlKeyManager; 
	
    public HttpClient samlHttpClient() {
        return new HttpClient(new MultiThreadedHttpConnectionManager());
    }
	
    // Initialization of the velocity engine
//    @Bean
    public VelocityEngine samlVelocityEngine() {
        try {
            
            VelocityEngine velocityEngine = new VelocityEngine();
            velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_INSTANCE, LoggerFactory.getLogger("samlVelocity"));
            velocityEngine.setProperty(RuntimeConstants.ENCODING_DEFAULT, "UTF-8");
            velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            velocityEngine.setProperty("classpath.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            velocityEngine.init();
            return velocityEngine;
        } catch (Exception e) {
            throw new RuntimeException("Error configuring velocity", e);
        }
    }
	
    @Bean(initMethod = "initialize")
    public StaticBasicParserPool samlParserPool() {
        return new StaticBasicParserPool();
    }

    @Bean(name = "parserPoolHolder")
    public ParserPoolHolder samlParserPoolHolder() {
        return new ParserPoolHolder();
    }

    @Bean
    public SAMLUserDetailsService samlUserDetailsService() {
        if ("spid".equals(samlType)) {
            return new AACSAMLUserDetailsService("fiscalNumber");
        } else if ("adp".equals(samlType)) {
            //TODO set mapping for username
            return new AACSAMLUserDetailsService();
        } else {
            return new AACSAMLUserDetailsService();
        }
    }

    @Bean
    public SAMLAuthenticationProvider samlAuthenticationProvider() {
        SAMLAuthenticationProvider samlAuthenticationProvider = new SAMLAuthenticationProvider();
        samlAuthenticationProvider.setUserDetails(samlUserDetailsService());
        samlAuthenticationProvider.setForcePrincipalAsString(false);
        return samlAuthenticationProvider;
    }

    @Bean
    public SAMLContextProviderImpl samlContextProvider() {
        return new SAMLContextProviderImpl();
    }

    @Bean
    public static SAMLBootstrap samlBootstrap() {
        return new AACSAMLBootstrap();
    }

    @Bean
    public SAMLDefaultLogger samlLogger() {
        return new SAMLDefaultLogger();
    }

    @Bean
    public SAMLEntryPoint samlEntryPoint() {
        SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();
        samlEntryPoint.setDefaultProfileOptions(defaultWebSSOProfileOptions());
        return samlEntryPoint;
    }
    
    // SAML webSSO
    @Bean
    public WebSSOProfileConsumer webSSOprofileConsumer() {
        if ("adp".equals(samlType)) {
            return new ADPWebSSOProfileConsumer();
        } else {
            return new WebSSOProfileConsumerImpl();
        }
    }

    @Bean
    public WebSSOProfileConsumerHoKImpl hokWebSSOprofileConsumer() {
        return new WebSSOProfileConsumerHoKImpl();
    }

    @Bean
    public WebSSOProfile webSSOprofile() {
        if ("spid".equals(samlType)) {
            return new SPIDWebSSOProfile();
        } else {
            return new WebSSOProfileImpl();
        }
    }

    @Bean
    public WebSSOProfileConsumerHoKImpl hokWebSSOProfile() {
        return new WebSSOProfileConsumerHoKImpl();
    }

    // SAML ECP
    @Bean
    public WebSSOProfileECPImpl ecpprofile() {
        return new WebSSOProfileECPImpl();
    }

    // SAML logout
    @Bean
    public SingleLogoutProfile samlLogoutprofile() {
        return new SingleLogoutProfileImpl();
    }
    
    // TODO rewrite in a proper builder
    @Bean
    public WebSSOProfileOptions defaultWebSSOProfileOptions() {
        WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();

        if ("spid".equals(samlType)) {
//            webSSOProfileOptions.setBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
            webSSOProfileOptions.setPassive(null); // not allowed for spid
            webSSOProfileOptions.setIncludeScoping(false);

            webSSOProfileOptions.setNameID("urn:oasis:names:tc:SAML:2.0:nameid-format:transient");

            webSSOProfileOptions.setAuthnContextComparison(AuthnContextComparisonTypeEnumeration.MINIMUM);

            List<String> authnContexts = new ArrayList<>();
            authnContexts.add("https://www.spid.gov.it/SpidL2");

            webSSOProfileOptions.setAuthnContexts(authnContexts);

            webSSOProfileOptions.setAssertionConsumerIndex(0);

        } else if ("adp".equals(samlType)) {
            //HTTP-POST does not work??
            webSSOProfileOptions.setBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);

            webSSOProfileOptions.setIncludeScoping(true);
            // no need to declare for adp
//        webSSOProfileOptions.setAllowedIDPs(Collections.singleton("2"));

            webSSOProfileOptions.setAllowCreate(false);
            webSSOProfileOptions.setNameID("urn:oasis:names:tc:SAML:2.0:nameid-format:transient");

            webSSOProfileOptions.setAuthnContextComparison(AuthnContextComparisonTypeEnumeration.EXACT);

            List<String> authnContexts = new ArrayList<>();
//        authnContexts.add("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
            authnContexts.add("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken");
            authnContexts.add("urn:oasis:names:tc:SAML:2.0:ac:classes:SecureRemotePassword");
            authnContexts.add("urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard");

            webSSOProfileOptions.setAuthnContexts(authnContexts);

            webSSOProfileOptions.setAssertionConsumerIndex(0);

        } else {
            webSSOProfileOptions.setIncludeScoping(false);
            webSSOProfileOptions.setAllowCreate(true);
            webSSOProfileOptions.setNameID("urn:oasis:names:tc:SAML:2.0:nameid-format:transient");

            webSSOProfileOptions.setAssertionConsumerIndex(0);

        }

        // TODO implement dynamic relayState with session binding
        webSSOProfileOptions.setRelayState("123asfasdf24");

        return webSSOProfileOptions;

    }
    
    //IDP
    @Bean
    @Qualifier("metadata")
    public MetadataManager samlMetadata() throws MetadataProviderException {
        List<MetadataProvider> providers = new ArrayList<MetadataProvider>();
        providers.add(ssoMetadataProvider());
        CachingMetadataManager manager = new CachingMetadataManager(providers);
        manager.setDefaultExtendedMetadata(spExtendedMetadata());
        manager.setRefreshCheckInterval(5*60*1000l); //5 minutes
        
//        //register sp metadata NOW
//        //TODO rework, interferes with autowiring..
//        manager.setHostedSPName(samlSpEntityId);
//        MetadataGenerator generator = samlMetadataGenerator();
//        EntityDescriptor descriptor = generator.generateMetadata();
//        ExtendedMetadata extendedMetadata = generator.generateExtendedMetadata();
//        MetadataMemoryProvider memoryProvider = new MetadataMemoryProvider(descriptor);
//        memoryProvider.initialize();
//        MetadataProvider metadataProvider = new ExtendedMetadataDelegate(memoryProvider, extendedMetadata);
//
//        manager.addMetadataProvider(metadataProvider);
//        manager.setHostedSPName(descriptor.getEntityID());

        return manager;
    }

    
    public ExtendedMetadata idpExtendedMetadata() {
        ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        extendedMetadata.setIdpDiscoveryEnabled(true);
        extendedMetadata.setSigningAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        extendedMetadata.setSignMetadata(true);
        extendedMetadata.setEcpEnabled(true);
        return extendedMetadata;
    }
    
    @Bean
    @Qualifier("idp-sso")
    public ExtendedMetadataDelegate ssoMetadataProvider()
            throws MetadataProviderException {
//TODO implement background refresh as per example
//        HTTPMetadataProvider httpMetadataProvider = new HTTPMetadataProvider(
//                this.backgroundTaskTimer, httpClient(), samlIdpMetadata);
//        httpMetadataProvider.setParserPool(parserPool());
//        ExtendedMetadataDelegate extendedMetadataDelegate = new ExtendedMetadataDelegate(httpMetadataProvider,
//                idpExtendedMetadata());
//        extendedMetadataDelegate.setMetadataTrustCheck(false);
//        extendedMetadataDelegate.setMetadataRequireSignature(false);
//        backgroundTaskTimer.purge();
//        return extendedMetadataDelegate;
        
        HTTPMetadataProvider httpMetadataProvider = new HTTPMetadataProvider(samlIdpMetadata, 2000);
        httpMetadataProvider.setParserPool(samlParserPool());
        ExtendedMetadataDelegate extendedMetadataDelegate = new ExtendedMetadataDelegate(httpMetadataProvider,
              idpExtendedMetadata());
        //we need to disable trust check for adp
        //TODO enable for others
        extendedMetadataDelegate.setMetadataTrustCheck(false);
        extendedMetadataDelegate.setMetadataRequireSignature(false);
        
        return extendedMetadataDelegate;
    }
    
    // IDP Discovery Service
    @Bean
    public SAMLDiscovery samlIDPDiscovery() {
        SAMLDiscovery idpDiscovery = new SAMLDiscovery();
        idpDiscovery.setIdpSelectionPath("/saml/discovery");
        return idpDiscovery;
    }
    
    // SP
    @Bean
    public ExtendedMetadata spExtendedMetadata() {
        ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        extendedMetadata.setIdpDiscoveryEnabled(false);
        extendedMetadata.setSigningAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        extendedMetadata.setSignMetadata(true);
        extendedMetadata.setEcpEnabled(false);
//        extendedMetadata.setAlias(samlSpName); //disabled, needs mapping on URLS
        extendedMetadata.setLocal(true);
        return extendedMetadata;
    }

//    @Bean
    public MetadataGenerator samlMetadataGenerator() {
        if ("spid".equals(samlType)) {
            MetadataGenerator metadataGenerator = new SPIDMetadataGenerator(samlSpName, samlSpDescription);
            metadataGenerator.setEntityId(samlSpEntityId);
            metadataGenerator.setExtendedMetadata(spExtendedMetadata());
            metadataGenerator.setIncludeDiscoveryExtension(false);
            metadataGenerator.setKeyManager(samlKeyManager);
            metadataGenerator.setNameID(Collections.singletonList("TRANSIENT"));
            metadataGenerator.setEntityBaseURL(applicationURL);
            return metadataGenerator;
        } else {
            MetadataGenerator metadataGenerator = new MetadataGenerator();
            metadataGenerator.setEntityId(samlSpEntityId);
            metadataGenerator.setExtendedMetadata(spExtendedMetadata());
            metadataGenerator.setIncludeDiscoveryExtension(false);
            metadataGenerator.setKeyManager(samlKeyManager);
            metadataGenerator.setNameID(Collections.singletonList("TRANSIENT"));
            metadataGenerator.setEntityBaseURL(applicationURL);
            return metadataGenerator;
        }
    }
    
    
    // Handlers
//    @Bean
    public SimpleUrlLogoutSuccessHandler samlSuccessLogoutHandler() {
        SimpleUrlLogoutSuccessHandler successLogoutHandler = new SimpleUrlLogoutSuccessHandler();
        successLogoutHandler.setDefaultTargetUrl("/");
        return successLogoutHandler;
    }

    // Logout handler terminating local session
//    @Bean
    public SecurityContextLogoutHandler samlLogoutHandler() {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(true);
        logoutHandler.setClearAuthentication(true);
        return logoutHandler;
    }

    // Handler deciding where to redirect user after successful login
//    @Bean
//    public SavedRequestAwareAuthenticationSuccessHandler samlSuccessRedirectHandler() {
//        SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler = new SavedRequestAwareAuthenticationSuccessHandler();
//        successRedirectHandler.setDefaultTargetUrl("/");
//        return successRedirectHandler;
//    }
    public SamlLoginSuccessHandler samlSuccessRedirectHandler() {
        SamlLoginSuccessHandler successRedirectHandler = new SamlLoginSuccessHandler("/");
        successRedirectHandler.setDefaultTargetUrl("/eauth/"+samlType);
        return successRedirectHandler;
    }

    // Handler deciding where to redirect user after failed login
//    @Bean
    public SimpleUrlAuthenticationFailureHandler samlAuthenticationFailureHandler() {
        SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
        failureHandler.setUseForward(true);
        failureHandler.setDefaultFailureUrl("/error");
        return failureHandler;
    }
    
	//Filters
    @Bean
    public MetadataDisplayFilter samlMetadataDisplayFilter() {
        return new MetadataDisplayFilter();
    }
    
    @Bean
    public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {
        SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter();
        samlWebSSOProcessingFilter.setFilterProcessesUrl("/saml/SSO");
        samlWebSSOProcessingFilter.setAuthenticationManager(authenticationManager());
        samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(samlSuccessRedirectHandler());
        samlWebSSOProcessingFilter.setAuthenticationFailureHandler(samlAuthenticationFailureHandler());
        return samlWebSSOProcessingFilter;
    }

    @Bean
    public MetadataGeneratorFilter samlMetadataGeneratorFilter() {
        return new MetadataGeneratorFilter(samlMetadataGenerator());
    }

    // Filter processing incoming logout messages
    // First argument determines URL user will be redirected to after successful
    // global logout
    @Bean
    public SAMLLogoutProcessingFilter samlLogoutProcessingFilter() {
        return new SAMLLogoutProcessingFilter(samlSuccessLogoutHandler(),
                samlLogoutHandler());
    }

    // Overrides default logout processing filter with the one processing SAML
    // messages
    @Bean
    public SAMLLogoutFilter samlLogoutFilter() {
        return new SAMLLogoutFilter(samlSuccessLogoutHandler(),
                new LogoutHandler[] { samlLogoutHandler() },
                new LogoutHandler[] { samlLogoutHandler() });
    }

    // Bindings
    private ArtifactResolutionProfile artifactResolutionProfile() {
        final ArtifactResolutionProfileImpl artifactResolutionProfile = new ArtifactResolutionProfileImpl(samlHttpClient());
        artifactResolutionProfile.setProcessor(new SAMLProcessorImpl(soapBinding()));
        return artifactResolutionProfile;
    }

    @Bean
    public HTTPArtifactBinding artifactBinding(ParserPool parserPool, VelocityEngine velocityEngine) {
        return new HTTPArtifactBinding(parserPool, velocityEngine, artifactResolutionProfile());
    }

    @Bean
    public HTTPSOAP11Binding soapBinding() {
        return new HTTPSOAP11Binding(samlParserPool());
    }

    @Bean
    public HTTPPostBinding httpPostBinding() {
        return new HTTPPostBinding(samlParserPool(), samlVelocityEngine());
    }

    @Bean
    public HTTPRedirectDeflateBinding httpRedirectDeflateBinding() {
        return new HTTPRedirectDeflateBinding(samlParserPool());
    }

    @Bean
    public HTTPSOAP11Binding httpSOAP11Binding() {
        return new HTTPSOAP11Binding(samlParserPool());
    }

    @Bean
    public HTTPPAOS11Binding httpPAOS11Binding() {
        return new HTTPPAOS11Binding(samlParserPool());
    }

    // Processor
    @Bean
    public SAMLProcessorImpl processor() {
        Collection<SAMLBinding> bindings = new ArrayList<SAMLBinding>();
        bindings.add(httpRedirectDeflateBinding());
        bindings.add(httpPostBinding());
        bindings.add(artifactBinding(samlParserPool(), samlVelocityEngine()));
        bindings.add(httpSOAP11Binding());
        bindings.add(httpPAOS11Binding());
        return new SAMLProcessorImpl(bindings);
    }
    
    
    /**
     * Define the security filter chain in order to support SSO Auth by using SAML
     * 2.0
     * 
     * @return Filter chain proxy
     * @throws Exception
     */
    @Bean
    public FilterChainProxy samlFilter() throws Exception {
        List<SecurityFilterChain> chains = new ArrayList<SecurityFilterChain>();
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/login/**"),
                samlEntryPoint()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/logout/**"),
                samlLogoutFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/metadata/**"),
                samlMetadataDisplayFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSO/**"),
                samlWebSSOProcessingFilter()));
//        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSOHoK/**"),
//                samlWebSSOHoKProcessingFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SingleLogout/**"),
                samlLogoutProcessingFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/discovery/**"),
                samlIDPDiscovery()));
        
        return new FilterChainProxy(chains);
    }
	/*
	 * Configure 
	 */
	
	
//	@Override
//	public void configure(WebSecurity web) throws Exception {
//	    web.ignoring()
//	    .antMatchers("/eauth/authorize/**");
//	}
	
	@Override
	public void configure(HttpSecurity http) throws Exception {
	    
	    
	    
		http
//		.anonymous().disable()
		.authorizeRequests()
			.antMatchers("/eauth/authorize/**").permitAll()
			.antMatchers("/oauth/authorize", "/eauth/**").authenticated()
			.antMatchers("/", "/dev**", "/account/**").hasAnyAuthority((restrictedAccess ? "ROLE_MANAGER" : "ROLE_USER"), "ROLE_ADMIN")
			.antMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN")
		.and()
		.exceptionHandling()
			.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
			.accessDeniedPage("/accesserror")
		.and()
		.logout()
			.logoutSuccessHandler(logoutSuccessHandler()).permitAll()
		.and()
		.rememberMe()
			.key(remembermeKey)
			.rememberMeServices(rememberMeServices())
		.and()
		.csrf()
			.disable()
			//TODO move away, we do not want this on every call
	    .addFilterBefore(samlMetadataGeneratorFilter(), ChannelProcessingFilter.class)
		.addFilterBefore(extOAuth2Filter(), BasicAuthenticationFilter.class);
	}

	/**
	 * @return
	 */
	private LogoutSuccessHandler logoutSuccessHandler() {
		SimpleUrlLogoutSuccessHandler handler = new SimpleUrlLogoutSuccessHandler();
		handler.setDefaultTargetUrl("/");
		handler.setTargetUrlParameter("target");
		return handler;
	}

	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(getInternalUserDetailsService());
		//saml auth provider
		//TODO rework and properly separate oauth/saml
		   auth
           .authenticationProvider(samlAuthenticationProvider());
	}

    @Bean
    protected ContextExtender contextExtender() {
        return new ContextExtender(applicationURL, configMatchPorts, configMatchSubDomains);
    }

	@Configuration
	@EnableAuthorizationServer
	protected class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {
		@Autowired
		private DataSource dataSource;

		@Autowired
		private ApprovalStore approvalStore;
		@Autowired	
		private ClientDetailsService clientDetailsService;

		@Autowired
		private UserApprovalHandler userApprovalHandler;

		@Autowired
		private AuthenticationManager authenticationManager;

		@Autowired	
		private ClientDetailsRepository clientDetailsRepository;

		@Autowired
		private ProviderServiceAdapter providerServiceAdapter;
		
		@Autowired
		private UserManager userManager;
		
		@Autowired
		@Qualifier("appTokenServices")
		private AuthorizationServerTokenServices resourceServerTokenServices;
		
		@Autowired
		private AutoJdbcAuthorizationCodeServices authorizationCodeServices;

		@Bean
		public AutoJdbcAuthorizationCodeServices getAuthorizationCodeServices() throws PropertyVetoException {
			return new AutoJdbcAuthorizationCodeServices(dataSource, authCodeValidity);
		}

		@Bean
		public OAuth2RequestFactory getOAuth2RequestFactory() throws PropertyVetoException {
			AACOAuth2RequestFactory<UserManager> result = new AACOAuth2RequestFactory<>();
			return result;

		}

		@Bean
		public OAuthFlowExtensions getFlowExtensions() throws PropertyVetoException {
			return new WebhookOAuthFlowExtensions();
		}

		
		@Bean
		public UserApprovalHandler getUserApprovalHandler() throws PropertyVetoException {
			UserApprovalHandler bean = new UserApprovalHandler();
			bean.setApprovalStore(approvalStore);
			bean.setClientDetailsService(clientDetailsService);
			bean.setRequestFactory(getOAuth2RequestFactory());
			bean.setFlowExtensions(getFlowExtensions());
			return bean;
		}

		Filter endpointFilter() {
			ClientCredentialsTokenEndpointFilter filter = new ClientCredentialsTokenEndpointFilter(
					clientDetailsRepository);
			filter.setAuthenticationManager(authenticationManager);
			// need to initialize success/failure handlers
			filter.afterPropertiesSet();
			return filter;
		}

		@Override
		public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
			clients.withClientDetails(getClientDetails());
		}

		@Override
		public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
			endpoints.tokenStore(tokenStore).userApprovalHandler(userApprovalHandler)
					.authenticationManager(authenticationManager)
					.requestFactory(getOAuth2RequestFactory())
					.requestValidator(new AACOAuth2RequestValidator())
					.tokenServices(resourceServerTokenServices)
					.authorizationCodeServices(authorizationCodeServices)			
					//set tokenGranter now to ensure all services are set
					.tokenGranter(tokenGranter(endpoints))
					.exceptionTranslator(new AACWebResponseExceptionTranslator());
		}

		@Override
		public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
			oauthServer.addTokenEndpointAuthenticationFilter(endpointFilter());
			// disable default endpoints: we enable access
			// because the endpoints are mapped to out custom controller returning 404
			oauthServer.tokenKeyAccess("permitAll()");
			oauthServer.checkTokenAccess("permitAll()");
		}
		
		

        private TokenGranter tokenGranter(final AuthorizationServerEndpointsConfigurer endpoints) {
            // build our own list of granters
            List<TokenGranter> granters = new ArrayList<TokenGranter>();
            // insert PKCE auth code granter as the first one to supersede basic authcode
            PKCEAwareTokenGranter pkceTokenGranter = new PKCEAwareTokenGranter(endpoints.getTokenServices(),
                    authorizationCodeServices,
                    endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory());
            if (oauth2PKCEAllowRefresh) {
                pkceTokenGranter.setAllowRefresh(true);
            }
            granters.add(pkceTokenGranter);

            // auth code 
            granters.add(new AuthorizationCodeTokenGranter(endpoints.getTokenServices(),
                    endpoints.getAuthorizationCodeServices(), endpoints.getClientDetailsService(),
                    endpoints.getOAuth2RequestFactory()));

            // refresh
            granters.add(new RefreshTokenGranter(endpoints.getTokenServices(), endpoints.getClientDetailsService(),
                    endpoints.getOAuth2RequestFactory()));

            // implicit
            granters.add(new ImplicitTokenGranter(endpoints.getTokenServices(),
                    endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory()));

            // client credentials
            ClientCredentialsTokenGranter clientCredentialsTokenGranter = new ClientCredentialsTokenGranter(endpoints.getTokenServices(),
                    endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory());
            if (oauth2ClientCredentialsAllowRefresh) {
                clientCredentialsTokenGranter.setAllowRefresh(true);
            }
            granters.add(clientCredentialsTokenGranter);

            // resource owner password
            if (authenticationManager != null) {
                ResourceOwnerPasswordTokenGranter passwordTokenGranter = new ResourceOwnerPasswordTokenGranter(authenticationManager, endpoints.getTokenServices(),
                        endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory());
                if (!oauth2ResourceOwnerPasswordAllowRefresh) {
                    passwordTokenGranter.setAllowRefresh(false);
                }                
                granters.add(passwordTokenGranter);
            }

            // custom native flow support
            granters.add(new NativeTokenGranter(userManager, providerServiceAdapter, endpoints.getTokenServices(),
                    endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory(), "native"));
            return new CompositeTokenGranter(granters);

//			List<TokenGranter> granters = new ArrayList<TokenGranter>(Arrays.asList(endpoints.getTokenGranter()));
//			granters.add(0, new ImplicitTokenGranter(endpoints.getTokenServices(), endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory()));
//			// insert PKCE auth code granter as the first one, before default implementation
//			granters.add(0,new PKCEAwareTokenGranter(endpoints.getTokenServices(), endpoints.getAuthorizationCodeServices(), endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory()));
//			// custom native flow support
//			granters.add(new NativeTokenGranter(userManager, providerServiceAdapter, endpoints.getTokenServices(), endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory(), "native"));
//			return new CompositeTokenGranter(granters);
        }
    }

	@Bean
	protected ResourceServerConfiguration profileResources() {
		ResourceServerConfiguration resource = new ResourceServerConfiguration() {
			public void setConfigurers(List<ResourceServerConfigurer> configurers) {
				super.setConfigurers(configurers);
			}
		};
		resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
			public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
				resources.resourceId(null);
			}

			public void configure(HttpSecurity http) throws Exception {
				http.antMatcher("/*profile/**").authorizeRequests()
						.antMatchers(HttpMethod.OPTIONS, "/*profile/**").permitAll()
						.antMatchers("/basicprofile/all/{{\\w+}}").access("#oauth2.hasScope('"+Config.SCOPE_BASIC_PROFILE_ALL+"')")
						.antMatchers("/basicprofile/all").access("#oauth2.hasScope('"+Config.SCOPE_BASIC_PROFILE_ALL+"')")
						.antMatchers("/basicprofile/profiles").access("#oauth2.hasScope('"+Config.SCOPE_BASIC_PROFILE_ALL+"')")
						.antMatchers("/basicprofile/me").access("#oauth2.hasScope('"+Config.SCOPE_BASIC_PROFILE+"')")
						.antMatchers("/accountprofile/profiles").access("#oauth2.hasScope('"+Config.SCOPE_ACCOUNT_PROFILE_ALL+"')")
						.antMatchers("/accountprofile/me").access("#oauth2.hasScope('"+Config.SCOPE_ACCOUNT_PROFILE+"')")				
						.and().csrf().disable();
			}
		}));
		resource.setOrder(4);
		return resource;
	}

	@Bean
	protected ResourceServerConfiguration restRegistrationResources() {
		ResourceServerConfiguration resource = new ResourceServerConfiguration() {
			public void setConfigurers(List<ResourceServerConfigurer> configurers) {
				super.setConfigurers(configurers);
			}
		};
		resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
			public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
				resources.resourceId(null);
			}

			Filter endpointFilter() throws Exception {
				ClientCredentialsRegistrationFilter filter = new ClientCredentialsRegistrationFilter(clientDetailsRepository);
				filter.setFilterProcessesUrl("/internal/register/rest");
				filter.setAuthenticationManager(authenticationManagerBean());
				// need to initialize success/failure handlers
				filter.afterPropertiesSet();
				return filter;
			}
			
			public void configure(HttpSecurity http) throws Exception {
				http.addFilterAfter(endpointFilter(), BasicAuthenticationFilter.class);
				
				http.antMatcher("/internal/register/rest").authorizeRequests().anyRequest()
						.fullyAuthenticated().and().csrf().disable();
			}

		}));
		resource.setOrder(5);
		return resource;
	}

	@Bean
	protected ResourceServerConfiguration rolesResources() {
		ResourceServerConfiguration resource = new ResourceServerConfiguration() {
			public void setConfigurers(List<ResourceServerConfigurer> configurers) {
				super.setConfigurers(configurers);
			}
		};
		resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
			public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
				resources.resourceId(null);
			}

			public void configure(HttpSecurity http) throws Exception {
				http.antMatcher("/*userroles/**").authorizeRequests()
						.antMatchers(HttpMethod.OPTIONS, "/*userroles/**").permitAll()
						.antMatchers("/userroles/me").access("#oauth2.hasScope('"+Config.SCOPE_ROLE+"')")
						.antMatchers(HttpMethod.GET, "/userroles/role").access("#oauth2.hasScope('"+Config.SCOPE_ROLES_READ+"')")
						.antMatchers(HttpMethod.GET, "/userroles/user/{\\w+}").access("#oauth2.hasScope('"+Config.SCOPE_ROLES_READ+"')")						
						.antMatchers(HttpMethod.PUT, "/userroles/user/{\\w+}").access("#oauth2.hasScope('"+Config.SCOPE_ROLES_WRITE+"')")
						.antMatchers(HttpMethod.DELETE, "/userroles/user/{\\w+}").access("#oauth2.hasScope('"+Config.SCOPE_ROLES_WRITE+"')")
						.antMatchers("/userroles/client/{\\w+}").access("#oauth2.hasScope('"+Config.SCOPE_CLIENT_ROLES_READ_ALL+"')")
						.antMatchers("/userroles/client").access("#oauth2.hasScope('"+Config.SCOPE_CLIENT_ROLES_READ_ALL+"')")
						.antMatchers("/userroles/token/{\\w+}").access("#oauth2.hasScope('"+Config.SCOPE_CLIENT_ROLES_READ_ALL+"')")
						.and().csrf().disable();
			}

		}));
		resource.setOrder(6);
		return resource;
	}	
	
	@Bean
	protected ResourceServerConfiguration wso2ClientResources() {
		ResourceServerConfiguration resource = new ResourceServerConfiguration() {
			public void setConfigurers(List<ResourceServerConfigurer> configurers) {
				super.setConfigurers(configurers);
			}
		};
		resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
			public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
				resources.resourceId(null);
			}

			public void configure(HttpSecurity http) throws Exception {
				http.antMatcher("/wso2/client/**").authorizeRequests().anyRequest()
						.access("#oauth2.hasScope('"+Config.SCOPE_CLIENTMANAGEMENT+"')").and().csrf().disable();
			}

		}));
		resource.setOrder(7);
		return resource;
	}

	@Bean
	protected ResourceServerConfiguration wso2APIResources() {
		ResourceServerConfiguration resource = new ResourceServerConfiguration() {
			public void setConfigurers(List<ResourceServerConfigurer> configurers) {
				super.setConfigurers(configurers);
			}
		};
		resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
			public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
				resources.resourceId(null);
			}

			public void configure(HttpSecurity http) throws Exception {
				http.antMatcher("/wso2/resources/**").authorizeRequests().anyRequest()
						.access("#oauth2.hasScope('"+Config.SCOPE_APIMANAGEMENT+"')").and().csrf().disable();
			}

		}));
		resource.setOrder(8);
		return resource;
	}
	
	
	@Bean
	protected ResourceServerConfiguration authorizationResources() {
		ResourceServerConfiguration resource = new ResourceServerConfiguration() {
			public void setConfigurers(List<ResourceServerConfigurer> configurers) {
				super.setConfigurers(configurers);
			}
		};
		resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
			public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
				resources.resourceId(null);
			}

			public void configure(HttpSecurity http) throws Exception {
				http.antMatcher("/*authorization/**").authorizeRequests().antMatchers(HttpMethod.OPTIONS, "/*authorization/**")
						.permitAll()
						.antMatchers("/authorization/**").access("#oauth2.hasScope('"+Config.SCOPE_AUTH_MANAGE+"')")
						.antMatchers("/authorization/*/schema/**").access("#oauth2.hasScope('"+Config.SCOPE_AUTH_SCHEMA_MANAGE+"')")
						.and().csrf().disable();
			}

		}));
		resource.setOrder(9);
		return resource;
	}		

	
	@Bean
	protected ResourceServerConfiguration userInfoResources() {
		ResourceServerConfiguration resource = new ResourceServerConfiguration() {
			public void setConfigurers(List<ResourceServerConfigurer> configurers) {
				super.setConfigurers(configurers);
			}
		};
		resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
			public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
				resources.resourceId(null);
			}

			public void configure(HttpSecurity http) throws Exception {
				http.antMatcher(UserInfoEndpoint.USERINFO_URL).authorizeRequests()
						.antMatchers(HttpMethod.OPTIONS, UserInfoEndpoint.USERINFO_URL).permitAll()
						.antMatchers(UserInfoEndpoint.USERINFO_URL).access("#oauth2.hasScope('"+Config.SCOPE_OPENID+"')")
						.and().csrf().disable();
			}

		}));
		resource.setOrder(11);
		return resource;
	}
	
	@Bean
	protected ResourceServerConfiguration tokenIntrospectionResources() {
		ResourceServerConfiguration resource = new ResourceServerConfiguration() {
			public void setConfigurers(List<ResourceServerConfigurer> configurers) {
				super.setConfigurers(configurers);
			}
		};
		resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
			public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
				resources.resourceId(null);
			}

			public void configure(HttpSecurity http) throws Exception {
				http.antMatcher(TokenIntrospectionEndpoint.TOKEN_INTROSPECTION_URL).authorizeRequests()
						.antMatchers(TokenIntrospectionEndpoint.TOKEN_INTROSPECTION_URL).hasAnyAuthority("ROLE_CLIENT", "ROLE_CLIENT_TRUSTED")
						.and().httpBasic()
						.and().userDetailsService(new OAuthClientUserDetails(clientDetailsRepository));
			}
		}));
		resource.setOrder(12);
		return resource;
	}
	
    @Bean
    protected ResourceServerConfiguration tokenRevocationResources() {
        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
                super.setConfigurers(configurers);
            }
        };
        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
                resources.resourceId(null);
            }

            public void configure(HttpSecurity http) throws Exception {
                http.antMatcher(TokenRevocationEndpoint.TOKEN_REVOCATION_URL).authorizeRequests()
                        .antMatchers(TokenRevocationEndpoint.TOKEN_REVOCATION_URL).hasAnyAuthority("ROLE_CLIENT", "ROLE_CLIENT_TRUSTED")
                        .and().httpBasic()
                        .and().userDetailsService(new OAuthClientUserDetails(clientDetailsRepository));
            }
        }));
        resource.setOrder(13);
        return resource;
    }
    
    @Bean
    protected ResourceServerConfiguration serviceManagementResources() {
		ResourceServerConfiguration resource = new ResourceServerConfiguration() {
			public void setConfigurers(List<ResourceServerConfigurer> configurers) {
				super.setConfigurers(configurers);
			}
		};
		resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
			public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
				resources.resourceId(null);
			}

			public void configure(HttpSecurity http) throws Exception {
				http.regexMatcher("/api/services(.)*").authorizeRequests().regexMatchers(HttpMethod.OPTIONS, "/api/services(.)*")
				.permitAll()
				.regexMatchers("/api/services(.)*").access("#oauth2.hasAnyScope('"+Config.SCOPE_SERVICEMANAGEMENT+"', '"+Config.SCOPE_SERVICEMANAGEMENT_USER+"')")
				.and().csrf().disable();
			}

		}));
		resource.setOrder(14);
		return resource;
    }
    
    @Bean
    protected ResourceServerConfiguration claimManagementResources() {
		ResourceServerConfiguration resource = new ResourceServerConfiguration() {
			public void setConfigurers(List<ResourceServerConfigurer> configurers) {
				super.setConfigurers(configurers);
			}
		};
		resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
			public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
				resources.resourceId(null);
			}

			public void configure(HttpSecurity http) throws Exception {
				http.antMatcher("/api/claims/**").authorizeRequests().antMatchers(HttpMethod.OPTIONS, "/api/claims/**")
				.permitAll()
				.antMatchers("/api/claims/**").access("#oauth2.hasAnyScope('"+Config.SCOPE_CLAIMMANAGEMENT+"', '"+Config.SCOPE_CLAIMMANAGEMENT_USER+"')")
				.and().csrf().disable();
			}

		}));
		resource.setOrder(15);
		return resource;
    }
    
    

    @Bean
    protected ResourceServerConfiguration apiKeyResources() {
        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
                super.setConfigurers(configurers);
            }
        };
        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
                resources.resourceId(null);
            }

            public void configure(HttpSecurity http) throws Exception {
                http.regexMatcher("/apikeycheck(.*)").authorizeRequests()
                        .regexMatchers("/apikeycheck(.*)").hasAnyAuthority("ROLE_CLIENT", "ROLE_CLIENT_TRUSTED")
                        .and().httpBasic()
                        .and().userDetailsService(new OAuthClientUserDetails(clientDetailsRepository));
                
                http.csrf().disable();
            }

        }));
        resource.setOrder(16);
        return resource;
    }   
    
    @Bean
    protected ResourceServerConfiguration apiKeyClientResources() {
        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
                super.setConfigurers(configurers);
            }
        };
        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
                resources.resourceId(null);
            }

            public void configure(HttpSecurity http) throws Exception {
                http.regexMatcher("/apikey/client(.*)").authorizeRequests()
                        .antMatchers("/apikey/client/me").access("#oauth2.hasScope('"+Config.SCOPE_APIKEY_CLIENT+"')")
                        .antMatchers("/apikey/client/{\\w+}").access("#oauth2.hasAnyScope('"+Config.SCOPE_APIKEY_CLIENT+"','"+Config.SCOPE_APIKEY_CLIENT_ALL+"')")
                        .antMatchers(HttpMethod.POST, "/apikey/client").access("#oauth2.hasAnyScope('"+Config.SCOPE_APIKEY_CLIENT+"','"+Config.SCOPE_APIKEY_CLIENT_ALL+"')")
                        .and().userDetailsService(new OAuthClientUserDetails(clientDetailsRepository));
                
                http.csrf().disable();
            }

        }));
        resource.setOrder(17);
        return resource;
    }
    
    @Bean
    protected ResourceServerConfiguration apiKeyUserResources() {
        ResourceServerConfiguration resource = new ResourceServerConfiguration() {
            public void setConfigurers(List<ResourceServerConfigurer> configurers) {
                super.setConfigurers(configurers);
            }
        };
        resource.setConfigurers(Arrays.<ResourceServerConfigurer>asList(new ResourceServerConfigurerAdapter() {
            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
                resources.resourceId(null);
            }

            public void configure(HttpSecurity http) throws Exception {
                http.regexMatcher("/apikey/user(.*)").authorizeRequests()
                        .antMatchers("/apikey/user/me").access("#oauth2.hasScope('"+Config.SCOPE_APIKEY_USER+"')")
                        .antMatchers("/apikey/user/{\\w+}").access("#oauth2.hasScope('"+Config.SCOPE_APIKEY_USER+"')")
                        .antMatchers(HttpMethod.POST, "/apikey/user").access("#oauth2.hasAnyScope('"+Config.SCOPE_APIKEY_USER+"','"+Config.SCOPE_APIKEY_USER_CLIENT+"')")
                        .and().csrf().disable();
            }

        }));
        resource.setOrder(18);
        return resource;
    }    
    
}
