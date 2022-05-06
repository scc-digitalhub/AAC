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

import java.util.HashMap;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/*
 * OpenAPI config is last
 * 
 */
@Configuration
@Order(30)
public class OpenAPIConfig {

//    @Autowired
//    private OpenAPIConf conf;

    @Value("${application.url}")
    private String AUTH_SERVER;

    @Bean
    @ConfigurationProperties("openapi")
    public OpenAPIConf getConf() {
        return new OpenAPIConf();
    }

    @Bean
    public OpenAPI aacOpenAPI(OpenAPIConf conf) {
        return new OpenAPI()
                .info(new Info()
                        .title("AAC API")
                        .description("Authorization and Authentication Control APIs")
                        .version(conf.version)
                        .license(new License().name(conf.license).url(conf.licenseUrl))
                        .contact(new Contact().url(conf.contact.get("url")).name(conf.contact.get("name"))
                                .email(conf.contact.get("email"))))
                .components(new Components()
                        .addSecuritySchemes("oauth2",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.OAUTH2)
//                                .scheme("bearer")
                                        .flows(new OAuthFlows()
                                                .authorizationCode(
                                                        new OAuthFlow().tokenUrl(AUTH_SERVER + "/oauth/token")
                                                                .authorizationUrl(AUTH_SERVER + "/oauth/authorize")
                                                                .scopes(scopes()))
                                                .clientCredentials(
                                                        new OAuthFlow().tokenUrl(AUTH_SERVER + "/oauth/token")))));
    }

    private Scopes scopes() {
        return new Scopes()
                .addString("aac.api.provider", "Manage identity and attribute providers")
                .addString("aac.api.attributes", "Manage custom attribute sets definitions")
                .addString("aac.api.audit", "Read audit log")
                .addString("aac.api.clientapp", "Manage client apps")
                .addString("aac.api.realm", "Manage realms")
                .addString("aac.api.services", "Manage custom services")
                .addString("aac.api.users", "Manage user identities, accounts, and attributes");
    }

    @Bean
    public GroupedOpenApi apiCore() {
        return GroupedOpenApi.builder()
                .group("AAC Management API")
                .packagesToScan("it.smartcommunitylab.aac.api")
                .pathsToExclude("/api/realm", "/api/realm/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement().addList("oauth2"));
                    return operation;
                })
                .build();
    }

    @Bean
    public GroupedOpenApi adminCore() {
        return GroupedOpenApi.builder()
                .group("AAC Admin API")
                .pathsToMatch("/api/realm", "/api/realm/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement().addList("oauth2"));
                    return operation;
                })
                .build();
    }

    @Bean
    public GroupedOpenApi oidc() {
        return GroupedOpenApi.builder()
                .group("AAC OpenID Connect API")
                .packagesToScan("it.smartcommunitylab.aac.openid")
                .addOpenApiCustomiser(customizer -> {
                    customizer.components(new Components()
                            .addSecuritySchemes("openid",
                                    new SecurityScheme().type(SecurityScheme.Type.OPENIDCONNECT).scheme("bearer")
                                            .openIdConnectUrl(AUTH_SERVER + "/.well-known/openid-configuration")));
                })
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement().addList("openid"));
                    return operation;
                })
                .build();
    }

    @Bean
    public GroupedOpenApi oauth() {
        return GroupedOpenApi.builder()
                .group("AAC OAuth API")
                .packagesToScan("it.smartcommunitylab.aac.oauth")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement().addList("oauth2"));
                    return operation;
                })
                .build();
    }

    @Bean
    public GroupedOpenApi profile() {
        return GroupedOpenApi.builder()
                .group("AAC User Profile API")
                .packagesToScan("it.smartcommunitylab.aac.profiles")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement().addList("oauth2"));
                    return operation;
                })
                .build();
    }

    @Bean
    public GroupedOpenApi roles() {
        return GroupedOpenApi.builder()
                .group("AAC Role API")
                .packagesToScan("it.smartcommunitylab.aac.roles")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement().addList("oauth2"));
                    return operation;
                })
                .build();
    }

    public static class OpenAPIConf {
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