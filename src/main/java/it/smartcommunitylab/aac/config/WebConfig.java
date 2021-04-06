package it.smartcommunitylab.aac.config;

import java.util.Locale;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

/*
 * Configure web container before security  
 */
@Configuration
@Order(10)
public class WebConfig extends WebMvcConfigurerAdapter {

    @Bean
    public CookieLocaleResolver getLocaleResolver() {
        CookieLocaleResolver bean = new CookieLocaleResolver();
        bean.setDefaultLocale(Locale.ITALY);
        return bean;
    }

    @Bean
    FilterRegistrationBean shallowEtagBean() {
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
        // configure a sane path mapping, avoid huge security holes with:
        // * spring security considering /about /about/ /about.any correctly as
        // different
        // * spring MVC considering all those the same
        // result is only /about is protected by antMatcher, all the other variants are
        // open to the world
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
