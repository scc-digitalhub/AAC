package it.smartcommunitylab.aac.config;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;

import it.smartcommunitylab.aac.authority.AnonymousAuthorityHandler;
import it.smartcommunitylab.aac.authority.AuthorityHandler;
import it.smartcommunitylab.aac.authority.AuthorityHandlerContainer;
import it.smartcommunitylab.aac.authority.DefaultAuthorityHandler;
import it.smartcommunitylab.aac.authority.FBAuthorityHandler;
import it.smartcommunitylab.aac.authority.GoogleAuthorityHandler;
import it.smartcommunitylab.aac.oauth.CachedResourceStorage;

@Configuration 
@EnableWebMvc
@ComponentScan("it.smartcommunitylab.aac")
@EnableAutoConfiguration
public class AACConfig extends WebMvcConfigurerAdapter {

	@Bean public RequestContextListener requestContextListener(){
	    return new RequestContextListener();
	} 	
	
//	@Bean
//	public ClientCredentialsFilter getClientCredentialsFilter() throws PropertyVetoException {
//		ClientCredentialsFilter ccf = new ClientCredentialsFilter("/internal/register/rest");
//		ccf.setAuthenticationManager(getAuthenticationManager());
//		return ccf;
//	}	

//	@Bean
//	public JdbcServices getJdbcServices() throws PropertyVetoException {
//		return new JdbcServices(getDataSource());
//	}
	
	@Bean
	public CachedResourceStorage getResourceStorage() {
		return new CachedResourceStorage();
	}
	
	
	@Bean
	public ResourceBundleMessageSource getMessageSource() {
		ResourceBundleMessageSource bean = new ResourceBundleMessageSource();
		bean.setBasename("resources/internal");
		return bean;
	}
	
	@Bean
	public AuthorityHandlerContainer getAuthorityHandlerContainer() {
		Map<String, AuthorityHandler> map = Maps.newTreeMap();
		
		GoogleAuthorityHandler gh = new GoogleAuthorityHandler();
		map.put("googlelocal", gh);
		FBAuthorityHandler fh = new FBAuthorityHandler();
		map.put("facebooklocal", fh);
		AnonymousAuthorityHandler ah = new AnonymousAuthorityHandler();
		map.put("anonymous", ah);
		
		AuthorityHandlerContainer bean = new AuthorityHandlerContainer(map);
		
		return bean;
	}

	@Bean
	public DefaultAuthorityHandler getDefaultHandler() {
		return new DefaultAuthorityHandler();
	}

	@Bean
	public CookieLocaleResolver getLocaleResolver() {
		CookieLocaleResolver bean = new CookieLocaleResolver();
		bean.setDefaultLocale(Locale.ITALY);
		return bean;
	}	
	
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }	
	
//    @Bean
//    public ContentNegotiationManager getContentNegotiationManager() {
//    	ContentNegotiationManager bean = new ContentNegotiationManager();
//    	
//    	bean.
//    }
    
    @Bean
    public ViewResolver viewResolver() {
    	ContentNegotiatingViewResolver bean = new ContentNegotiatingViewResolver();
 
    	InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setViewClass(JstlView.class);
        viewResolver.setPrefix("/WEB-INF/jsp/");
        viewResolver.setSuffix(".jsp");
 
        List<ViewResolver> viewResolvers = Lists.newArrayList();
        viewResolvers.add(viewResolver);
        bean.setViewResolvers(viewResolvers);
        
        List<View> views = Lists.newArrayList();
        MappingJackson2JsonView view = new MappingJackson2JsonView();
        views.add(view);        
        bean.setDefaultViews(views);
        
        return bean;
    }	

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("PUT", "DELETE", "GET", "POST").allowedOrigins("*");
	}	
	
	
}
