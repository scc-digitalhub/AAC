package it.smartcommunitylab.aac.config;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;

/*
 * Configure web container before security  
 */
@Configuration
@Order(20)
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public CookieLocaleResolver getLocaleResolver() {
        CookieLocaleResolver bean = new CookieLocaleResolver();
        bean.setDefaultLocale(Locale.ITALY);
        return bean;
    }

    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagBean() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filter = new FilterRegistrationBean<>();
        filter.setFilter(new ShallowEtagHeaderFilter());
        filter.addUrlPatterns("/html/*", "/js/*", "/css/*", "/fonts/*", "/lib/*", "/italia/*");
        filter.addUrlPatterns("/logo");
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
        // disable suffix match, as of 5.3 is set as false by default
//        configurer.setUseSuffixPatternMatch(false);
        configurer.setUseTrailingSlashMatch(false);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // configure a sane path mapping by disabling content negotiation via extensions
        // the default breaks every single mapping which receives a path ending with
        // '.x', like 'user.roles.me'
        configurer
        // disable path extension, as of 5.3 is false by default
//                .favorPathExtension(false)
                .favorParameter(false);

        // add mediatypes
        configurer
                .ignoreAcceptHeader(false)
                .defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType(MediaType.APPLICATION_JSON.getSubtype(),
                        MediaType.APPLICATION_JSON)
                .mediaType(SystemKeys.MEDIA_TYPE_YML.getSubtype(), SystemKeys.MEDIA_TYPE_YML)
                .mediaType(SystemKeys.MEDIA_TYPE_YAML.getSubtype(), SystemKeys.MEDIA_TYPE_YAML);
    }

    /*
     * Yaml support (experimental)
     */

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter yamlConverter = new MappingJackson2HttpMessageConverter(yamlObjectMapper);
        yamlConverter.setSupportedMediaTypes(Arrays.asList(SystemKeys.MEDIA_TYPE_YML, SystemKeys.MEDIA_TYPE_YAML));
        converters.add(yamlConverter);
    }

}
