/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/
package it.smartcommunitylab.aac.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import springfox.documentation.builders.AuthorizationCodeGrantBuilder;
import springfox.documentation.builders.OAuthBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.BasicAuth;
import springfox.documentation.service.ClientCredentialsGrant;
import springfox.documentation.service.Contact;
import springfox.documentation.service.GrantType;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.service.Tag;
import springfox.documentation.service.TokenEndpoint;
import springfox.documentation.service.TokenRequestEndpoint;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/*
 * Swagger config is last
 * 
 * TODO update to openAPI 3
 */
@Configuration
@Order(20)
@EnableSwagger2
public class SwaggerConfig {                       
	
	private static final String CLIENT_SECRET = "<your-client-secret>";
	private static final String CLIENT_ID = "<your-client-id>";

	@Autowired
	private SwaggerConf conf;
	
	@Value("${application.url}")
	private String AUTH_SERVER;
	
	@Bean
	@ConfigurationProperties("swagger")
	public SwaggerConf getConf(){
		return new SwaggerConf();
	}

    
    /***************** OAUTH2 AAC API **********/ 
    @Bean
    public Docket apiOAuth2() {
        return new Docket(DocumentationType.SWAGGER_2)
            .groupName("AAC OAuth2")
            .apiInfo(apiInfo(conf.title.get("AACOAuth2"), conf.description.get("AACOAuth2")))
            .select()
            .apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.aac.oauth.endpoint"))
            .build()
            .tags(new Tag("OAuth 2.0 Authorization Framework", "OAuth 2.0 Authorization Framework (IETF RFC6749)"))
            .tags(new Tag("OAuth 2.0 Authorization Server Metadata", "OAuth 2.0 Authorization Server Metadata (IETF RFC8414)"))
            .tags(new Tag("OAuth 2.0 Token Introspection", "OAuth 2.0 Token Introspection (IETF RFC7662)."))
            .tags(new Tag("OAuth 2.0 Token Revocation", "OAuth 2.0 Token Revocation (IETF RFC7009)"))
            .securitySchemes(Arrays.asList(
                    securitySchemeApp(aacClientApp())))
            .securityContexts(Arrays.asList(
                    securityContext(aacClientApp(), "(/oauth/revoke)|(/oauth/introspect)","application")
                    ));
    }
    
    private AuthorizationScope[] aacClientApp() {
        AuthorizationScope[] scopes = {};
        return scopes;
    }
 
    /*************************************************************************/     

    /***************** OPENID AAC API **********/ 
    @Bean
    public Docket apiOpenId() { 
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("AAC OpenID")
          .apiInfo(apiInfo(conf.title.get("AACOpenID"), conf.description.get("AACOpenID")))
          .select()                                  
          .apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.aac.openid.endpoint"))
          .build()
          .tags(new Tag("OpenID Connect Core", "OpenID Connect Core 1.0"))
          .tags(new Tag("OpenID Connect Discovery", "OpenID Connect Discovery 1.0"))
          .tags(new Tag("OpenID Connect Session Management", "OpenID Connect Session Management"))          
          .securitySchemes(Arrays.asList(
                  securitySchemeUser(aacScopesOpenID())
                  ))
          .securityContexts(Arrays.asList(
                  securityContext(aacScopesOpenID(), ".*/userinfo", "spring_oauth")
                  ));                                           
    }
 
    

    /***************** SERVICES AAC API **********/ 
    @Bean
    public Docket apiServices() { 
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("AAC Services, Scopes, Claims")
          .apiInfo(apiInfo(conf.title.get("AACServices"), conf.description.get("AACServices")))
          .select()                                  
          .apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.aac"))
          .paths(PathSelectors.regex("/api/services.*"))
          .build()
          .tags(new Tag("AAC Services", "Service, Scope, Claim definitions"))
          .securitySchemes(Arrays.asList(
                  securitySchemeUser(aacServiceMgmtUser()), 
                  securitySchemeApp(aacServiceMgmt())
                  ))
          .securityContexts(Arrays.asList(
        		  SecurityContext.builder()
          			.securityReferences(Arrays.asList(new SecurityReference("application", aacServiceMgmt()), new SecurityReference("spring_oauth", aacServiceMgmtUser())))
          			.forPaths(PathSelectors.regex("/api/services.*"))
          			.build()
                  ));                                           
    }
    
	private AuthorizationScope[] aacServiceMgmt() {
		AuthorizationScope[] scopes = { 
				new AuthorizationScope("servicemanagement", "Core service for managing service, scope, and claim definitions."), 
		};
		return scopes;
	}
	private AuthorizationScope[] aacServiceMgmtUser() {
		AuthorizationScope[] scopes = { 
				new AuthorizationScope("servicemanagement.me", "Core service for managing owned service, scope, and claim definitions."), 
		};
		return scopes;
	}

 
	/***************** CLAIMS AAC API **********/ 
    @Bean
    public Docket apiClaims() { 
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("AAC Custom User Claims")
          .apiInfo(apiInfo(conf.title.get("AACClaims"), conf.description.get("AACClaims")))
          .select()                                  
          .apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.aac"))
          .paths(PathSelectors.regex("/api/claims/.*"))
          .build()
          .tags(new Tag("AAC Claims", "Custom User Claims"))
          .securitySchemes(Arrays.asList(
                  securitySchemeUser(aacClaimMgmtUser()), 
                  securitySchemeApp(aacClaimMgmt())
                  ))
          .securityContexts(Arrays.asList(
        		  SecurityContext.builder()
          			.securityReferences(Arrays.asList(new SecurityReference("application", aacClaimMgmt()), new SecurityReference("spring_oauth", aacClaimMgmtUser())))
          			.forPaths(PathSelectors.regex("/api/claims/.*"))
          			.build()
                  ));                                           
    }
    
	private AuthorizationScope[] aacClaimMgmt() {
		AuthorizationScope[] scopes = { 
				new AuthorizationScope("claimmanagement", "Manage custom user claim values"), 
		};
		return scopes;
	}
	private AuthorizationScope[] aacClaimMgmtUser() {
		AuthorizationScope[] scopes = { 
				new AuthorizationScope("claimmanagement.me", "Manage custom user claims for the services owned by the user"), 
		};
		return scopes;
	}
    /*************************************************************************/         
    
	/***************** Core AAC API - PROFILES, TOKEN INTROSPECTION **********/ 
    @Bean
    public Docket apiAac() { 
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("AAC")
          .apiInfo(apiInfo(conf.title.get("AAC"), conf.description.get("AAC")))
          .select()                                  
          .apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.aac"))
          .paths(PathSelectors.regex("(/.*profile.*)|(/resources.*)"))
          .build()
          .securitySchemes(Arrays.asList(
        		  securitySchemeUser(aacScopesUser()), 
        		  securitySchemeApp(aacScopesApp())
        		  ))
          .securityContexts(Arrays.asList(
        		  securityContext(aacScopesUser(), ".*profile/me", "spring_oauth"),
        		  securityContext(aacScopesApp(), "(.*profile/all.*)|(.*profile/profiles)", "application")
        		  ));                                           
    }
    
	private AuthorizationScope[] aacScopesUser() {
		AuthorizationScope[] scopes = { 
				new AuthorizationScope("profile.basicprofile.me", "Basic profile of the current platform user. Read access only."),
				new AuthorizationScope("profile", "Basic user profile data (name, surname, email). Read access only."),
				new AuthorizationScope("email", "Basic user's email."), 
				new AuthorizationScope("profile.accountprofile.me", "Account profile of the current platform user. Read access only."), 
		};
		return scopes;
	}
    private AuthorizationScope[] aacScopesOpenID() {
        AuthorizationScope[] scopes = { 
                new AuthorizationScope("openid", "OpenID"),
                new AuthorizationScope("profile", "Basic user profile data (name, surname, email). Read access only."),
                new AuthorizationScope("email", "Basic user's email."), 
        };
        return scopes;
    }
	private AuthorizationScope[] aacScopesApp() {
		AuthorizationScope[] scopes = { 
				new AuthorizationScope("profile.basicprofile.all", "Basic profile of the platform users. Read access only."), 
				new AuthorizationScope("profile.accountprofile.all", "Account profile of the platform users. Read access only."), 
		};
		return scopes;
	}
	/*************************************************************************/ 
    

    /*************************** API Key Management API **********************/ 
    @Bean
    public Docket apiApiKey() {
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("AAC ApiKey")
          .apiInfo(apiInfo(conf.title.get("AACApiKey"), conf.description.get("AACApiKey")))
          .select()                                  
          .apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.aac.apikey.endpoint"))
          .build()                                     
          .tags(new Tag("AAC ApiKey", "AAC ApiKey"))
          .tags(new Tag("AAC Client ApiKey", "AAC ApiKey Client"))
          .tags(new Tag("AAC User ApiKey", "AAC ApiKey User"))          
          .securitySchemes(Arrays.asList(
                  securitySchemeUser(aacApiKeyMgmtHybrid()),                  
                  securitySchemeApp(aacApiKeyClientMgmt()),
                  securityBasicAuthApp()
                  ))
          .securityContexts(Arrays.asList(
                  securityContext(aacClientApp(), "/apikeycheck.*", "basicAuth"),
                  securityContext(aacApiKeyMgmtHybrid(), "/apikey/user.*", "spring_oauth"),                  
                  securityContext(aacApiKeyClientMgmt(), "/apikey/client.*", "application")
                  ));                                           
    }
    
    private AuthorizationScope[] aacApiKeyMgmt() {
        AuthorizationScope[] scopes = { 
                new AuthorizationScope("apikey.client.all", "Manage all client apikeys"), 
        };
        return scopes;
    }
    private AuthorizationScope[] aacApiKeyClientMgmt() {
        AuthorizationScope[] scopes = { 
                new AuthorizationScope("apikey.client.me", "Manage client apikeys"), 
        };
        return scopes;
    }    
    private AuthorizationScope[] aacApiKeyMgmtHybrid() {
        AuthorizationScope[] scopes = { 
                new AuthorizationScope("apikey.user.me", "Manage user apikeys"), 
                new AuthorizationScope("apikey.user.client", "Manage user client own apikeys"), 
        };
        return scopes;
    }    
    private AuthorizationScope[] aacApiKeyMgmtUser() {
        AuthorizationScope[] scopes = { 
                new AuthorizationScope("apikey.user.me", "Manage user apikeys"), 
        };
        return scopes;
    }    
    /*************************************************************************/ 
	
	
	
	/******************************* AUTHORIZATION API ***********************/ 
	@Bean
    public Docket apiAuthorization() { 
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("AACAuthorization")
          .apiInfo(apiInfo(conf.title.get("AACAuthorization"), conf.description.get("AACAuthorization")))
          .select()                                  
          .apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.aac"))
          .paths(PathSelectors.regex("/authorization.*"))
          .build()          
          .securitySchemes(Arrays.asList(
        		  securitySchemeApp(authorizationScopesApp())
        		  ))
          .securityContexts(Arrays.asList(
        		  securityContext(authorizationScopesApp(), "/authorization.*", "application")
        		  ));                                           
                                           
    }
	private AuthorizationScope[] authorizationScopesApp() {
		AuthorizationScope[] scopes = { 
				new AuthorizationScope("authorization.manage", "Modify authorizations"), 
				new AuthorizationScope("authorization.schema.manage", "Manage authorization schema"), 
		};
		return scopes;
	}
	
	/*************************************************************************/ 
    
    
	/******************************* ROLES API *******************************/ 
    @Bean
    public Docket apiRoles() { 
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("AACRoles")
          .apiInfo(apiInfo(conf.title.get("AACRoles"), conf.description.get("AACRoles")))
          .select()                                  
          .apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.aac"))
          .paths(PathSelectors.regex("/userroles.*"))
          .build()
          .securitySchemes(Arrays.asList(
        		  securitySchemeUser(rolesScopesUser()), 
        		  securitySchemeApp(rolesScopesApp())
        		  ))
          .securityContexts(Arrays.asList(
        		  securityContext(rolesScopesUser(), "/userroles/me", "spring_oauth"),
        		  securityContext(rolesScopesApp(), "(/userroles/user.*)|(/userroles/client.*)|(/userroles/token.*)|(/userroles/role)", "application")
        		  ));                                           

    }    
    
	private AuthorizationScope[] rolesScopesUser() {
		AuthorizationScope[] scopes = { 
				new AuthorizationScope("user.roles.me", "Read roles of the current user.")
		};
		return scopes;
	}
	private AuthorizationScope[] rolesScopesApp() {
		AuthorizationScope[] scopes = { 
				new AuthorizationScope("user.roles.write", "Modify the roles of the specified user within a tenant."), 
				new AuthorizationScope("user.roles.read", "Read the roles of the specified user within a tenant."), 
				new AuthorizationScope("user.roles.read.all", "Read the roles of any user."), 
				new AuthorizationScope("client.roles.read.all", "Read the roles of any app client."), 
		};
		return scopes;
	}
	/*************************************************************************/ 
    
    public ApiInfo apiInfo(String title, String description) {
        return new ApiInfo(
          title, 
          description, 
          conf.version, 
          null, 
          new Contact(conf.contact.get("name"), conf.contact.get("url"), conf.contact.get("email")), 
          conf.license, 
          conf.licenseUrl, 
          Collections.emptyList());
    }
    
	private SecurityScheme securitySchemeUser(AuthorizationScope[] scopes) {
		GrantType acGrantType = new AuthorizationCodeGrantBuilder()
				.tokenEndpoint(new TokenEndpoint(AUTH_SERVER + "/oauth/token", "oauthtoken"))
				.tokenRequestEndpoint(new TokenRequestEndpoint(AUTH_SERVER + "/eauth/authorize", CLIENT_ID, CLIENT_SECRET)).build();
		SecurityScheme oauth = new OAuthBuilder().name("spring_oauth")
				.grantTypes(Arrays.asList(acGrantType)).scopes(Arrays.asList(scopes)).build();
		return oauth;
	}
	private SecurityScheme securitySchemeApp(AuthorizationScope[] scopes) {
		GrantType ccGrantType = new ClientCredentialsGrant(AUTH_SERVER + "/oauth/token");

		SecurityScheme oauth = new OAuthBuilder().name("application")
				.grantTypes(Arrays.asList(ccGrantType)).scopes(Arrays.asList(scopes)).build();
		return oauth;
	}
	
	private SecurityScheme securityBasicAuthApp() {
	    return new BasicAuth("basicAuth");
	}
	
	private SecurityContext securityContext(AuthorizationScope[] scopes, String path, String type) {
        return SecurityContext.builder()
        		.securityReferences(Arrays.asList(new SecurityReference(type, scopes)))
        		.forPaths(PathSelectors.regex(path))
        		.build();
    }
    
    public static class SwaggerConf {
		private HashMap<String, String> title;
		private HashMap<String, String> description;
		private HashMap<String, String> contact;
		private String version;
		private String license;
		private String licenseUrl;
		
		public HashMap<String, String> getTitle() {
			return title;
		}
		public void setTitle(HashMap<String, String> title) {
			this.title = title;
		}
		public HashMap<String, String> getDescription() {
			return description;
		}
		public void setDescription(HashMap<String, String> description) {
			this.description = description;
		}
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
		public String getLicense() {
			return license;
		}
		public void setLicense(String license) {
			this.license = license;
		}
		public String getLicenseUrl() {
			return licenseUrl;
		}
		public void setLicenseUrl(String licenseUrl) {
			this.licenseUrl = licenseUrl;
		}
		public HashMap<String, String> getContact() {
			return contact;
		}
		public void setContact(HashMap<String, String> contact) {
			this.contact = contact;
		}
   }	
}