package it.smartcommunitylab.aac.config;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import com.google.common.collect.Maps;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.authority.AuthorityHandler;
import it.smartcommunitylab.aac.authority.AuthorityHandlerContainer;
import it.smartcommunitylab.aac.authority.DefaultAuthorityHandler;
import it.smartcommunitylab.aac.authority.FBAuthorityHandler;
import it.smartcommunitylab.aac.authority.InternalAuthorityHandler;
import it.smartcommunitylab.aac.oauth.CachedServiceScopeServices;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

@Configuration 
public class AACConfig extends WebMvcConfigurerAdapter {

	@Autowired
	private ClientDetailsRepository clientDetailsRepository;

	@Bean
	public CachedServiceScopeServices getResourceStorage() {
		return new CachedServiceScopeServices();
	}
	
	@Bean
	public AuthorityHandlerContainer getAuthorityHandlerContainer() {
		Map<String, AuthorityHandler> map = Maps.newTreeMap();
		map.put(Config.IDP_INTERNAL, getInternalHandler());
		FBAuthorityHandler fh = new FBAuthorityHandler();
		map.put("facebook", fh);
		AuthorityHandlerContainer bean = new AuthorityHandlerContainer(map);
		return bean;
	}

	@Bean
	public DefaultAuthorityHandler getDefaultHandler() {
		return new DefaultAuthorityHandler();
	}
	@Bean
	public InternalAuthorityHandler getInternalHandler() {
		return new InternalAuthorityHandler();
	}

	@Bean
	public CookieLocaleResolver getLocaleResolver() {
		CookieLocaleResolver bean = new CookieLocaleResolver();
		bean.setDefaultLocale(Locale.ITALY);
		return bean;
	}	

    @Bean
    FilterRegistrationBean shallowEtagBean () {
        FilterRegistrationBean filter = new FilterRegistrationBean();
        filter.setFilter(new ShallowEtagHeaderFilter());
        filter.addUrlPatterns("/html/*", "/js/*", "/css/*", "/fonts/*", "/lib/*", "/italia/*");
//        frb.setOrder(2);
        return filter;
    }
	   
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("PUT", "DELETE", "GET", "POST").allowedOrigins("*");
	}	
	
    @Override
    public void configurePathMatch(final PathMatchConfigurer configurer) {
        //configure a sane path mapping, avoid huge security holes with: 
        // * spring security considering /about /about/ /about.any correctly as different
        // * spring MVC considering all those the same
        // result is only /about is protected by antMatcher, all the other variants are open to the world
        configurer.setUseSuffixPatternMatch(false);
        configurer.setUseTrailingSlashMatch(false);
    }
    
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // configure a sane path mapping by disabling content negotiation via extensions
        // the default breaks every single mapping which receives a path ending with
        // '.x', like 'user.roles.me'
        configurer.favorPathExtension(false);
    }
}
