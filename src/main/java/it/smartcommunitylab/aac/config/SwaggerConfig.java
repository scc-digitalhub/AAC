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

import java.util.Collections;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {                       
	
	@Autowired
	private SwaggerConf conf;
	
	@Bean
	@ConfigurationProperties("swagger")
	public SwaggerConf getConf(){
		return new SwaggerConf();
	}

    @Bean
    public Docket apiApiKey() {
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("AACApiKey")
          .apiInfo(apiInfo(conf.title.get("AACApiKey"), conf.description.get("AACApiKey")))
          .select()                                  
          .apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.aac"))
          .paths(PathSelectors.regex("/apikey.*"))
          .build();                                           
    }
    @Bean
    public Docket apiAac() { 
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("AAC")
          .apiInfo(apiInfo(conf.title.get("AAC"), conf.description.get("AAC")))
          .select()                                  
          .apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.aac"))
          .paths(PathSelectors.regex("(/.*profile.*)|(/resources.*)"))
          .build();                                           
    }
    @Bean
    public Docket apiAuthorization() { 
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("AACAuthorization")
          .apiInfo(apiInfo(conf.title.get("AACAuthorization"), conf.description.get("AACAuthorization")))
          .select()                                  
          .apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.aac"))
          .paths(PathSelectors.regex("/authorization.*"))
          .build();                                           
    }
    @Bean
    public Docket apiRoles() { 
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("AACRoles")
          .apiInfo(apiInfo(conf.title.get("AACRoles"), conf.description.get("AACRoles")))
          .select()                                  
          .apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.aac"))
          .paths(PathSelectors.regex("/userroles.*"))
          .build();                                           
    }    
    
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