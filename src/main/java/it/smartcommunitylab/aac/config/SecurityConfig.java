package it.smartcommunitylab.aac.config;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.filter.CorsFilter;
import org.yaml.snakeyaml.Yaml;

import it.smartcommunitylab.aac.apimanager.APIProviderManager;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.manager.ProviderServiceAdapter;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsRowMapper;
import it.smartcommunitylab.aac.model.MockDataMappings;
import it.smartcommunitylab.aac.oauth.AutoJdbcAuthorizationCodeServices;
import it.smartcommunitylab.aac.oauth.AutoJdbcTokenStore;
import it.smartcommunitylab.aac.oauth.ClientCredentialsRegistrationFilter;
import it.smartcommunitylab.aac.oauth.ClientCredentialsTokenEndpointFilter;
import it.smartcommunitylab.aac.oauth.ContextExtender;
import it.smartcommunitylab.aac.oauth.CustomOAuth2RequestFactory;
import it.smartcommunitylab.aac.oauth.InternalPasswordEncoder;
import it.smartcommunitylab.aac.oauth.InternalUserDetailsRepo;
import it.smartcommunitylab.aac.oauth.MockDataAwareOAuth2SuccessHandler;
import it.smartcommunitylab.aac.oauth.NativeTokenGranter;
import it.smartcommunitylab.aac.oauth.NonRemovingTokenServices;
import it.smartcommunitylab.aac.oauth.OAuthProviders;
import it.smartcommunitylab.aac.oauth.OAuthProviders.ClientResources;
import it.smartcommunitylab.aac.oauth.UserApprovalHandler;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

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

	@Autowired
	OAuth2ClientContext oauth2ClientContext;

	@Autowired
	private ClientDetailsRepository clientDetailsRepository;

	@Autowired
	private DataSource dataSource;

	@Bean
	public AutoJdbcTokenStore getTokenStore() throws PropertyVetoException {
		return new AutoJdbcTokenStore(dataSource);
	}

	@Bean
	public JdbcClientDetailsService getClientDetails() throws PropertyVetoException {
		JdbcClientDetailsService bean = new JdbcClientDetailsService(dataSource);
		bean.setRowMapper(getClientDetailsRowMapper());
		return bean;
	}

	@Bean
	public UserDetailsService getInternalUserDetailsService() {
		return new InternalUserDetailsRepo();
	}

	@Bean
	public ClientDetailsRowMapper getClientDetailsRowMapper() {
		return new ClientDetailsRowMapper();
	}

	@Bean
	public APIProviderManager tokenEmitter() {
		return new APIProviderManager();
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

	private Filter extOAuth2Filter() throws IOException {
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

	private Filter extOAuth2Filter(ClientResources client, String path, String target) throws IOException {
		OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(path);

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

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/eauth/authorize/**").permitAll()
				.antMatchers("/oauth/authorize", "/eauth/**").authenticated().antMatchers("/", "/dev**")
				.hasAnyAuthority((restrictedAccess ? "ROLE_MANAGER" : "ROLE_USER"), "ROLE_ADMIN")
				.antMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN").antMatchers("/mgmt/**")
				.hasAnyAuthority("ROLE_PROVIDER").and().exceptionHandling()
				.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
				.accessDeniedPage("/accesserror").and().logout().logoutSuccessUrl("/login").permitAll().and().csrf()
				.disable().addFilterBefore(extOAuth2Filter(), BasicAuthenticationFilter.class);
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
		private TokenStore tokenStore;

		@Autowired
		private UserApprovalHandler userApprovalHandler;

		@Autowired
		private AuthenticationManager authenticationManager;

		@Autowired
		private ClientDetailsService clientDetailsService;
		@Autowired
		private ClientDetailsRepository clientDetailsRepository;

		@Autowired
		private ProviderServiceAdapter providerServiceAdapter;
		
		@Bean
		public AutoJdbcAuthorizationCodeServices getAuthorizationCodeServices() throws PropertyVetoException {
			return new AutoJdbcAuthorizationCodeServices(dataSource);
		}

		@Bean
		public OAuth2RequestFactory getOAuth2RequestFactory() throws PropertyVetoException {
			CustomOAuth2RequestFactory<UserManager> result = new CustomOAuth2RequestFactory<>();
			return result;

		}

		// @Bean("appTokenServices")

		public NonRemovingTokenServices getTokenServices() throws PropertyVetoException {
			NonRemovingTokenServices bean = new NonRemovingTokenServices();
			bean.setTokenStore(tokenStore);
			bean.setSupportRefreshToken(true);
			bean.setReuseRefreshToken(true);
			bean.setClientDetailsService(clientDetailsService);
			return bean;
		}

		@Bean
		public UserApprovalHandler getUserApprovalHandler() throws PropertyVetoException {
			UserApprovalHandler bean = new UserApprovalHandler();
			bean.setTokenStore(tokenStore);
			bean.setRequestFactory(getOAuth2RequestFactory());
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
			clients.jdbc(dataSource).clients(clientDetailsService);
		}

		@Override
		public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
			endpoints.tokenStore(tokenStore).userApprovalHandler(userApprovalHandler)
					.authenticationManager(authenticationManager)
					.tokenGranter(tokenGranter(endpoints))
					.requestFactory(getOAuth2RequestFactory())
					.tokenServices(getTokenServices());
		}

		@Override
		public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
			oauthServer.addTokenEndpointAuthenticationFilter(endpointFilter());
		}

		private TokenGranter tokenGranter(final AuthorizationServerEndpointsConfigurer endpoints) {
			List<TokenGranter> granters = new ArrayList<TokenGranter>(Arrays.asList(endpoints.getTokenGranter()));
			granters.add(new NativeTokenGranter(providerServiceAdapter, endpoints.getTokenServices(), endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory(), "native"));
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
				http.antMatcher("/*profile/**").authorizeRequests().antMatchers(HttpMethod.OPTIONS, "/*profile/**").permitAll()
						.antMatchers("/basicprofile/all/**").access("#oauth2.hasScope('profile.basicprofile.all')")
						.antMatchers("/basicprofile/profiles/**").access("#oauth2.hasScope('profile.basicprofile.all')")
						.antMatchers("/basicprofile/me").access("#oauth2.hasScope('profile.basicprofile.me')")
						.antMatchers("/accountprofile/profiles").access("#oauth2.hasScope('profile.accountprofile.all')")
						.antMatchers("/accountprofile/me").access("#oauth2.hasScope('profile.accountprofile.me')")				
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

	// @Bean
	// protected ResourceServerConfiguration apiMgmtResources() {
	// ResourceServerConfiguration resource = new ResourceServerConfiguration()
	// {
	// public void setConfigurers(List<ResourceServerConfigurer> configurers) {
	// super.setConfigurers(configurers);
	// }
	// };
	// resource.setConfigurers(Arrays.<ResourceServerConfigurer> asList(new
	// ResourceServerConfigurerAdapter() {
	// public void configure(ResourceServerSecurityConfigurer resources) throws
	// Exception { resources.resourceId(null); }
	// public void configure(HttpSecurity http) throws Exception {
	// http
	// .antMatcher("/mgmt/apis")
	// .authorizeRequests()
	// .antMatchers(HttpMethod.OPTIONS, "/mgmt/apis").permitAll()
	// .anyRequest().authenticated()
	// .and().csrf().disable();
	// }
	//
	// }));
	// resource.setOrder(6);
	// return resource;
	// }

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
				http.antMatcher("/*userroles/**").authorizeRequests().antMatchers(HttpMethod.OPTIONS, "/*userroles/**")
						.permitAll()
						.antMatchers("/userroles/me").access("#oauth2.hasScope('user.roles.me')")
						.antMatchers("/userroles/all/user").access("#oauth2.hasScope('user.roles.read.all')")
						.antMatchers(HttpMethod.GET, "/userroles/user").access("#oauth2.hasScope('user.roles.read')")
						.antMatchers(HttpMethod.PUT, "/userroles/user").access("#oauth2.hasScope('user.roles.write')")
						.antMatchers(HttpMethod.DELETE, "/userroles/user").access("#oauth2.hasScope('user.roles.write')")
						.antMatchers("/userroles/client").access("#oauth2.hasScope('client.roles.read.all')")
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
						.access("#oauth2.hasScope('clientmanagement')").and().csrf().disable();
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
						.access("#oauth2.hasScope('apimanagement')").and().csrf().disable();
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
						.antMatchers("/authorization/**").access("#oauth2.hasScope('authorization.manage')")
						.antMatchers("/authorization/*/schema/**").access("#oauth2.hasScope('authorization.schema.manage')")
						.and().csrf().disable();
			}

		}));
		resource.setOrder(9);
		return resource;
	}		
	

}
