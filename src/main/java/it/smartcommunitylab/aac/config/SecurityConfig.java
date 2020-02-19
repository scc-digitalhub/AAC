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
import java.util.List;

import javax.servlet.Filter;
import javax.sql.DataSource;

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
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.filter.CorsFilter;
import org.yaml.snakeyaml.Yaml;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.manager.FileEmailIdentitySource;
import it.smartcommunitylab.aac.manager.IdentitySource;
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
import it.smartcommunitylab.aac.oauth.PKCEAwareTokenGranter;
import it.smartcommunitylab.aac.oauth.UserApprovalHandler;
import it.smartcommunitylab.aac.oauth.UserDetailsRepo;
import it.smartcommunitylab.aac.oauth.WebhookOAuthFlowExtensions;
import it.smartcommunitylab.aac.oauth.endpoint.TokenIntrospectionEndpoint;
import it.smartcommunitylab.aac.oauth.endpoint.TokenRevocationEndpoint;
import it.smartcommunitylab.aac.openid.endpoint.UserInfoEndpoint;
import it.smartcommunitylab.aac.openid.service.OIDCTokenEnhancer;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

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
	}

	@Bean
	protected ContextExtender contextExtender() {
		return new ContextExtender();
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
			return new AutoJdbcAuthorizationCodeServices(dataSource);
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
			List<TokenGranter> granters = new ArrayList<TokenGranter>(Arrays.asList(endpoints.getTokenGranter()));
			// insert PKCE auth code granter as the first one, before default implementation
			granters.add(0,new PKCEAwareTokenGranter(endpoints.getTokenServices(), endpoints.getAuthorizationCodeServices(), endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory()));
			// custom native flow support
			granters.add(new NativeTokenGranter(userManager, providerServiceAdapter, endpoints.getTokenServices(), endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory(), "native"));
			return new CompositeTokenGranter(granters);
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
				http.regexMatcher("/apikey(.*)").authorizeRequests()
						.regexMatchers("/apikey(.*)").hasAnyAuthority("ROLE_CLIENT", "ROLE_CLIENT_TRUSTED")
						.and().httpBasic()
						.and().userDetailsService(new OAuthClientUserDetails(clientDetailsRepository));
				
				http.csrf().disable();
			}

		}));
		resource.setOrder(10);
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
}
