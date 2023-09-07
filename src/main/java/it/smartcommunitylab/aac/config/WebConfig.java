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

import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.LanguageHandlerInterceptor;
import it.smartcommunitylab.aac.templates.TemplateHandlerInterceptor;
import java.util.Arrays;
import java.util.List;
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
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;

/*
 * Configure web container before security
 */
@Configuration
@Order(20)
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TemplateHandlerInterceptor templateInterceptor;

    @Autowired
    private LocaleChangeInterceptor localeChangeInterceptor;

    @Autowired
    private LanguageHandlerInterceptor languageInterceptor;

    /*
     * ETag for cache
     */

    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagBean() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filter = new FilterRegistrationBean<>();
        filter.setFilter(new ShallowEtagHeaderFilter());
        filter.addUrlPatterns("/html/*", "/js/*", "/css/*", "/fonts/*", "/lib/*", "/italia/*");
        filter.addUrlPatterns("/logo");
        //        frb.setOrder(2);
        return filter;
    }

    /*
     * CORS
     */

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedMethods("PUT", "DELETE", "GET", "POST").allowedOrigins("*");
    }

    /*
     * MVC config
     */

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
        configurer.favorParameter(false); //                .favorPathExtension(false) // disable path extension, as of 5.3 is false by default

        // add mediatypes
        configurer
            .ignoreAcceptHeader(false)
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType(MediaType.APPLICATION_JSON.getSubtype(), MediaType.APPLICATION_JSON)
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

    /*
     * Template interceptor
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor);
        registry.addInterceptor(languageInterceptor);
        registry.addInterceptor(templateInterceptor);
    }

    /*
     * Static console resolvers for webpack style
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // user console
        registry
            .addResourceHandler("/console/user/**")
            .addResourceLocations("classpath:/console/user/")
            .setCachePeriod(60 * 60 * 24 * 365)/* one year */
            .resourceChain(true)
            .addResolver(new EncodedResourceResolver())
            .addResolver(new PathResourceResolver());
    }
}
