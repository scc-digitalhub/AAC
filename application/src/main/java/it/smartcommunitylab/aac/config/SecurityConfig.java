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

import it.smartcommunitylab.aac.core.auth.ExtendedLogoutSuccessHandler;
import it.smartcommunitylab.aac.core.auth.RealmAwareAuthenticationEntryPoint;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwarePathUriBuilder;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.crypto.InternalPasswordEncoder;
import it.smartcommunitylab.aac.password.auth.InternalPasswordResetOnAccessFilter;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordEntityRepository;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.realms.service.RealmService;
import it.smartcommunitylab.aac.tos.TosOnAccessFilter;
import it.smartcommunitylab.aac.users.service.UserService;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.CompositeFilter;

/*
 * Security config for AAC UI
 *
 * Should be after all api/endpoint config to works as catch-all for remaining requests
 */

@Configuration
@Order(30)
@EnableConfigurationProperties
public class SecurityConfig {

    //    @Value("classpath:/testdata.yml")
    //    private Resource dataMapping;

    @Value("${application.url}")
    private String applicationURL;

    private static final String LOGINPATH = "/login";
    private static final String LOGOUTPATH = "/logout";
    private static final String LOGOPATH = "/logo";
    private static final String TERMSPATH = "/terms";

    @Autowired
    private RealmAwarePathUriBuilder realmUriBuilder;

    @Autowired
    private InternalUserPasswordEntityRepository passwordRepository;

    @Autowired
    private ProviderConfigRepository<PasswordIdentityProviderConfig> internalPasswordIdentityProviderConfigRepository;

    @Autowired
    private RealmService realmService;

    @Autowired
    private UserService userService;

    //    /*
    //     * rememberme
    //     */
    //    @Bean
    //    public PersistentTokenBasedRememberMeServices rememberMeServices() {
    //        AACRememberMeServices service = new AACRememberMeServices(remembermeKey, new UserDetailsRepo(userRepository),
    //                persistentTokenRepository());
    //        service.setCookieName(Config.COOKIE_REMEMBER_ME);
    //        service.setParameter(Config.PARAM_REMEMBER_ME);
    //        service.setTokenValiditySeconds(3600 * 24 * 60); // two month
    //        return service;
    //    }
    //
    //    @Bean
    //    public PersistentTokenRepository persistentTokenRepository() {
    //        JdbcTokenRepositoryImpl tokenRepositoryImpl = new JdbcTokenRepositoryImpl();
    //        tokenRepositoryImpl.setDataSource(dataSource);
    //        return tokenRepositoryImpl;
    //    }

    // disabled for upgrade, TODO fix cors
    //	@Bean
    //	public FilterRegistrationBean corsFilter() {
    //		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    //		CorsConfiguration config = new CorsConfiguration();
    //		config.setAllowCredentials(true);
    //		config.addAllowedOrigin("*");
    //		config.addAllowedHeader("*");
    //		config.addAllowedMethod("*");
    //		source.registerCorsConfiguration("/**", config);
    //		FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
    //		bean.setOrder(0);
    //		return bean;
    //	}

    @Bean
    public InternalPasswordEncoder getInternalPasswordEncoder() {
        return new InternalPasswordEncoder();
    }

    @Order(30)
    @Bean("securityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
            // whitelist error
            .antMatchers("/error")
            .permitAll()
            // whitelist login pages
            .antMatchers(LOGINPATH, LOGOUTPATH)
            .permitAll()
            .antMatchers("/-/{realm}" + LOGINPATH)
            .permitAll()
            .antMatchers("/-/{realm}" + TERMSPATH, TERMSPATH + "/reject")
            .permitAll()
            .antMatchers("/-/{realm}" + LOGOPATH)
            .permitAll()
            // whitelist auth providers pages (login,registration etc)
            //                .antMatchers("/auth/**").permitAll()
            // TODO remove tech-specific paths
            .antMatchers("/webauthn/**")
            .permitAll()
            // anything else requires auth
            .anyRequest()
            .authenticated()
            .and()
            .exceptionHandling()
            // use a realm aware entryPoint
            //                .authenticationEntryPoint(new RealmAwareAuthenticationEntryPoint("/login"))
            //                .defaultAuthenticationEntryPointFor(
            //                        oauth2AuthenticationEntryPoint(oauth2ClientDetailsService, loginPath),
            //                        new AntPathRequestMatcher("/oauth/**"))
            .defaultAuthenticationEntryPointFor(
                realmAuthEntryPoint(LOGINPATH, realmUriBuilder),
                new AntPathRequestMatcher("/**")
            )
            .accessDeniedPage("/accesserror")
            .and()
            .logout(logout ->
                logout
                    .logoutUrl(LOGOUTPATH)
                    .logoutRequestMatcher(new AntPathRequestMatcher(LOGOUTPATH))
                    .logoutSuccessHandler(logoutSuccessHandler(realmUriBuilder))
                    .permitAll()
            )
            //                .and()
            //                .rememberMe()
            //                .key(remembermeKey)
            //                .rememberMeServices(rememberMeServices())
            //                .and()
            .csrf()
            //                .disable()
            .ignoringAntMatchers("/logout", "/console/**", "/account/**")
            .and()
            //                // TODO replace with filterRegistrationBean and explicitely map urls
            //                .addFilterBefore(new ExpiredUserAuthenticationFilter(), BasicAuthenticationFilter.class);
            .addFilterAfter(buildSessionFilters(), BasicAuthenticationFilter.class)
            // we always want a session here
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.ALWAYS);

        return http.build();
    }

    /*
     * BUG hotfix: disable error filter because it doesn't work with dummyrequests
     * and/or multiple filter chains TODO remove when upstream fixes the issue
     *
     * https://github.com/spring-projects/spring-security/issues/11055#issuecomment-
     * 1098061598
     *
     */
    //    @Bean
    //    public static BeanFactoryPostProcessor removeErrorSecurityFilter() {
    //        return beanFactory -> ((DefaultListableBeanFactory) beanFactory)
    //                .removeBeanDefinition("errorPageSecurityInterceptor");
    //    }

    @Bean
    public CompositeLogoutHandler logoutHandler() {
        List<LogoutHandler> handlers = new ArrayList<>();
        SecurityContextLogoutHandler contextLogoutHandler = new SecurityContextLogoutHandler();
        contextLogoutHandler.setClearAuthentication(true);
        contextLogoutHandler.setInvalidateHttpSession(true);
        handlers.add(contextLogoutHandler);

        // cookie clearing
        String[] cookieNames = { "JSESSIONID", "csrftoken" };
        CookieClearingLogoutHandler cookieLogoutHandler = new CookieClearingLogoutHandler(cookieNames);
        handlers.add(cookieLogoutHandler);

        // TODO define tokenRepository as bean and use for csrf
        //        CsrfLogoutHandler csrfLogoutHandler = new CsrfLogoutHandler(csrfTokenRepository);
        //        handlers.add(csrfLogoutHandler);
        // TODO add remember me
        // localStorage clear - TODO add to httpSecurity handlers above
        LogoutHandler clearSiteLogoutHandler = new HeaderWriterLogoutHandler(
            new ClearSiteDataHeaderWriter(ClearSiteDataHeaderWriter.Directive.STORAGE)
        );

        handlers.add(clearSiteLogoutHandler);
        return new CompositeLogoutHandler(handlers);
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler(RealmAwarePathUriBuilder uriBuilder) {
        // TODO update dedicated, leverage OidcClientInitiatedLogoutSuccessHandler
        ExtendedLogoutSuccessHandler handler = new ExtendedLogoutSuccessHandler(LOGINPATH);
        handler.setRealmUriBuilder(uriBuilder);
        handler.setDefaultTargetUrl("/");
        handler.setTargetUrlParameter("target");
        return handler;
    }

    //    @Bean
    //    @Override
    //    @Primary
    //    public AuthenticationManager authenticationManagerBean() throws Exception {
    ////        return extendedAuthenticationManager();
    //        return authManager;
    //    }

    private RealmAwareAuthenticationEntryPoint realmAuthEntryPoint(
        String loginPath,
        RealmAwarePathUriBuilder uriBuilder
    ) {
        RealmAwareAuthenticationEntryPoint entryPoint = new RealmAwareAuthenticationEntryPoint(loginPath);
        entryPoint.setUseForward(false);
        entryPoint.setRealmUriBuilder(uriBuilder);

        return entryPoint;
    }

    private Filter buildSessionFilters() {
        InternalPasswordResetOnAccessFilter passwordChangeFilter = new InternalPasswordResetOnAccessFilter(
            passwordRepository,
            internalPasswordIdentityProviderConfigRepository
        );

        TosOnAccessFilter tosFilter = new TosOnAccessFilter(realmService, userService);

        // build a virtual filter chain as composite filter
        ArrayList<Filter> filters = new ArrayList<>();
        filters.add(passwordChangeFilter);
        filters.add(tosFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }
}
