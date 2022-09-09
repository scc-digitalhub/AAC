package it.smartcommunitylab.aac.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.CompositeFilter;

import it.smartcommunitylab.aac.core.ExtendedUserAuthenticationManager;
import it.smartcommunitylab.aac.core.authorities.AuthorityService;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfig;
import it.smartcommunitylab.aac.core.provider.ProviderConfig;
import it.smartcommunitylab.aac.core.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.openid.model.OIDCUserIdentity;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProvider;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;

@Configuration
@Order(17)
public class AuthConfig {

    @Autowired
//    private IdentityProviderAuthorityService identityProviderAuthorityService;
    private AuthorityService<IdentityProviderAuthority<?, ?, ?, ?>> identityProviderAuthorityService;

    @Autowired
    private ExtendedUserAuthenticationManager authManager;

    @Value("${application.url}")
    private String applicationURL;

    @Bean
    @Order(17)
    @Qualifier("authSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.requestMatcher(getRequestMatcher())
                .authorizeRequests()
                .anyRequest().permitAll()
                .and()
                .authenticationManager(authManager)
                .csrf()
                .ignoringRequestMatchers(buildAuthoritiesCorsMatcher())
                .and()
                .addFilterBefore(
                        buildAuthoritiesFilters(),
                        BasicAuthenticationFilter.class)
                // we always want a session here
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS);

        return http.build();
    }

    private Filter buildAuthoritiesFilters() {
        List<Filter> filters = new ArrayList<>();

        // build filters for every authority
        Collection<IdentityProviderAuthority<?, ?, ?, ?>> authorities = identityProviderAuthorityService
                .getAuthorities();

        for (IdentityProviderAuthority<?, ?, ?, ?> authority : authorities) {
            // build filters for this authority via filterProvider from authority itself
            FilterProvider provider = authority.getFilterProvider();
            if (provider != null) {
                // we expect a list of filters
                Collection<Filter> pfs = provider.getFilters();
                if (filters != null) {
                    for (Filter filter : pfs) {
                        // register authManager for authFilters
                        if (filter instanceof AbstractAuthenticationProcessingFilter) {
                            ((AbstractAuthenticationProcessingFilter) filter).setAuthenticationManager(authManager);
                        }

                        filters.add(filter);
                    }
                }
            }
        }

        // build a virtual filter chain as composite filter
        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    private RequestMatcher buildAuthoritiesCorsMatcher() {
        List<RequestMatcher> antMatchers = new ArrayList<>();

        identityProviderAuthorityService
                .getAuthorities().forEach(authority -> {
                    FilterProvider provider = authority.getFilterProvider();
                    if (provider != null) {
                        Collection<String> patterns = provider.getCorsIgnoringAntMatchers();
                        if (patterns != null) {
                            for (String pattern : patterns) {
                                antMatchers.add(new AntPathRequestMatcher(pattern));
                            }
                        }
                    }
                });

        return new OrRequestMatcher(antMatchers);
    }

    private RequestMatcher getRequestMatcher() {
        return new AntPathRequestMatcher(AUTH_URL);
    }

    private static final String AUTH_URL = "/auth/**";

}
