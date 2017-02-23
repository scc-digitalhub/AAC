package it.smartcommunitylab.aac.config;

import java.util.Locale;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import com.google.common.collect.Maps;

import it.smartcommunitylab.aac.authority.AnonymousAuthorityHandler;
import it.smartcommunitylab.aac.authority.AuthorityHandler;
import it.smartcommunitylab.aac.authority.AuthorityHandlerContainer;
import it.smartcommunitylab.aac.authority.DefaultAuthorityHandler;
import it.smartcommunitylab.aac.authority.FBAuthorityHandler;
import it.smartcommunitylab.aac.authority.GoogleAuthorityHandler;
import it.smartcommunitylab.aac.oauth.CachedResourceStorage;

@Configuration 
public class AACConfig extends WebMvcConfigurerAdapter {
	
	@Bean
	public CachedResourceStorage getResourceStorage() {
		return new CachedResourceStorage();
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
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("PUT", "DELETE", "GET", "POST").allowedOrigins("*");
	}	
	
}
