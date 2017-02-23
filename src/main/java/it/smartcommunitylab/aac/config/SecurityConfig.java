package it.smartcommunitylab.aac.config;

import java.beans.PropertyVetoException;
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
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.CompositeFilter;

import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.model.ClientDetailsRowMapper;
import it.smartcommunitylab.aac.oauth.AutoJdbcAuthorizationCodeServices;
import it.smartcommunitylab.aac.oauth.AutoJdbcTokenStore;
import it.smartcommunitylab.aac.oauth.ClientCredentialsTokenEndpointFilter;
import it.smartcommunitylab.aac.oauth.ContextExtender;
import it.smartcommunitylab.aac.oauth.ExtOAuth2SuccessHandler;
import it.smartcommunitylab.aac.oauth.InternalUserDetailsRepo;
import it.smartcommunitylab.aac.oauth.NonRemovingTokenServices;
import it.smartcommunitylab.aac.oauth.OAuthProviders;
import it.smartcommunitylab.aac.oauth.OAuthProviders.ClientResources;
import it.smartcommunitylab.aac.oauth.UserApprovalHandler;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

@Configuration 
@EnableOAuth2Client
@EnableConfigurationProperties
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("${application.url}")
	private String applicationURL;

	@Value("${security.restricted}")
	private boolean restrictedAccess;

	@Autowired
	OAuth2ClientContext oauth2ClientContext;

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
	public FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(filter);
		registration.setOrder(-100);
		return registration;
	}

	@Bean
	@ConfigurationProperties("oauth-providers")
	public OAuthProviders oauthProviders(){
		return new OAuthProviders();
	}
	
	private Filter extOAuth2Filter() {
		CompositeFilter filter = new CompositeFilter();
		List<Filter> filters = new ArrayList<>();
		List<ClientResources> providers = oauthProviders().getProviders();
		for (ClientResources client : providers) {
			String id = client.getProvider();
			filters.add(extOAuth2Filter(client, Utils.filterRedirectURL(id), "/eauth/"+id));
		}
		filter.setFilters(filters);
		return filter;
	}	
	
	private Filter extOAuth2Filter(ClientResources client, String path, String target) {
		OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(
				path);
		
		filter.setAuthenticationSuccessHandler(new ExtOAuth2SuccessHandler(target));
		
		OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(), oauth2ClientContext);
		filter.setRestTemplate(template);
		UserInfoTokenServices tokenServices = new UserInfoTokenServices(
				client.getResource().getUserInfoUri(), client.getClient().getClientId());
		tokenServices.setRestTemplate(template);
		filter.setTokenServices(tokenServices);
		return filter;
	}	
	
	@Override
	public void configure(HttpSecurity http) throws Exception {
		http
			.authorizeRequests()
				.antMatchers("/eauth/authorize/**").permitAll()
				.antMatchers("/oauth/authorize", "/eauth/**").authenticated()
				.antMatchers("/", "/dev**").hasAnyAuthority((restrictedAccess ? "ROLE_MANAGER" : "ROLE_USER"),"ROLE_ADMIN")
				.antMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN")
				.and().exceptionHandling()
					.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
					.accessDeniedPage("/accesserror")
				.and().logout()
					.logoutSuccessUrl("/login").permitAll()
				.and().csrf()
					.disable()
					.addFilterBefore(extOAuth2Filter(), BasicAuthenticationFilter.class)
					;
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

		@Bean
		public AutoJdbcAuthorizationCodeServices getAuthorizationCodeServices() throws PropertyVetoException {
			return new AutoJdbcAuthorizationCodeServices(dataSource);
		}	

		@Bean
		public OAuth2RequestFactory getOAuth2RequestFactory() throws PropertyVetoException {
			return new DefaultOAuth2RequestFactory(clientDetailsService);
		}

		@Bean
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
			ClientCredentialsTokenEndpointFilter filter = new ClientCredentialsTokenEndpointFilter(clientDetailsRepository);
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
			endpoints
			.tokenStore(tokenStore)
			.userApprovalHandler(userApprovalHandler)
			.authenticationManager(authenticationManager)
			;
		}		
        @Override
        public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
            oauthServer
            .addTokenEndpointAuthenticationFilter(endpointFilter())
            ;
        }
    }
	
	@Bean
	protected ResourceServerConfiguration profileResources() {
		ResourceServerConfiguration resource = new ResourceServerConfiguration() {	
			public void setConfigurers(List<ResourceServerConfigurer> configurers) {
				super.setConfigurers(configurers);
			}
		};
		resource.setConfigurers(Arrays.<ResourceServerConfigurer> asList(new ResourceServerConfigurerAdapter() {
			public void configure(ResourceServerSecurityConfigurer resources) throws Exception { resources.resourceId(null); }
			public void configure(HttpSecurity http) throws Exception {
				http
				.antMatcher("/*profile/**")
				.authorizeRequests()
				.antMatchers("/basicprofile/all").access("#oauth2.hasScope('profile.basicprofile.all')")
				.antMatchers("/basicprofile/me").access("#oauth2.hasScope('profile.basicprofile.me')")
				.antMatchers("/accountprofile/all").access("#oauth2.hasScope('profile.accountprofile.all')")
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
		resource.setConfigurers(Arrays.<ResourceServerConfigurer> asList(new ResourceServerConfigurerAdapter() {
			public void configure(ResourceServerSecurityConfigurer resources) throws Exception { resources.resourceId(null); }
			public void configure(HttpSecurity http) throws Exception {
				http
				.antMatcher("/internal/register/rest")
				.authorizeRequests()
				.anyRequest().access("#oauth2.hasScope('usermanagement')")
				.and().csrf().disable();
			}

		}));
		resource.setOrder(5);
		return resource;
	}
	
}
