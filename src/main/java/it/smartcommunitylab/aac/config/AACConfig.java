package it.smartcommunitylab.aac.config;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import com.google.common.collect.Maps;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.authority.AuthorityHandler;
import it.smartcommunitylab.aac.authority.AuthorityHandlerContainer;
import it.smartcommunitylab.aac.authority.DefaultAuthorityHandler;
import it.smartcommunitylab.aac.authority.FBAuthorityHandler;
import it.smartcommunitylab.aac.authority.FBNativeAuthorityHandler;
import it.smartcommunitylab.aac.authority.GoogleNativeAuthorityHandler;
import it.smartcommunitylab.aac.authority.InternalAuthorityHandler;
import it.smartcommunitylab.aac.authority.NativeAuthorityHandler;
import it.smartcommunitylab.aac.authority.NativeAuthorityHandlerContainer;
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
	public NativeAuthorityHandlerContainer getNativeAuthorityHandlerContainer() {
		Map<String, NativeAuthorityHandler> map = Maps.newTreeMap();
		
		GoogleNativeAuthorityHandler gh = new GoogleNativeAuthorityHandler(clientDetailsRepository);
		map.put("google", gh);
		FBNativeAuthorityHandler fh = new FBNativeAuthorityHandler();
		map.put("facebook", fh);
		
		NativeAuthorityHandlerContainer bean = new NativeAuthorityHandlerContainer(map);
		
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

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("PUT", "DELETE", "GET", "POST").allowedOrigins("*");
	}	
	
}
