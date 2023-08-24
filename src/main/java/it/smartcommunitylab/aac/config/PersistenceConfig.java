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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.attributes.provider.MapperAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.base.provider.config.AbstractProviderConfig;
import it.smartcommunitylab.aac.claims.ExtractorsRegistry;
import it.smartcommunitylab.aac.claims.InMemoryExtractorsRegistry;
import it.smartcommunitylab.aac.claims.ResourceClaimsExtractorProvider;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.AutoJDBCProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.ConfigurableProviderEntityService;
import it.smartcommunitylab.aac.core.service.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountEntityRepository;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.service.InternalJpaUserAccountService;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleIdentityProviderConfig;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAccount;
import it.smartcommunitylab.aac.oidc.persistence.OIDCUserAccountEntityRepository;
import it.smartcommunitylab.aac.oidc.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.oidc.service.OIDCJpaUserAccountService;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.password.service.InternalPasswordUserCredentialsService;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountEntityRepository;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.service.SamlJpaUserAccountService;
import it.smartcommunitylab.aac.scope.InMemoryScopeRegistry;
import it.smartcommunitylab.aac.scope.ScopeProvider;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredentialsEntityRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsServiceConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnConfigTranslatorRepository;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserCredentialsService;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;

@Configuration
@Order(2)
public class PersistenceConfig {

    @Value("${application.url}")
    private String applicationUrl;

    @Value("${persistence.repository.providerConfig}")
    private String providerConfigRepository;

    @Value("${security.session.cookie.sameSite}")
    private String sessionCookieSameSite;

    @Value("${security.session.cookie.secure}")
    private Boolean sessionCookieSecure;

    @Autowired
    private DataSource dataSource;

    /*
     * Object mappers
     */

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    @Qualifier("yamlObjectMapper")
    public ObjectMapper yamlObjectMapper() {
        //        YAMLFactory factory = new YAMLFactory()
        //                .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
        //                .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);

        YAMLFactory factory = yamlFactory();
        ObjectMapper yamlObjectMapper = new ObjectMapper(factory);
        yamlObjectMapper.registerModule(new JavaTimeModule());
        yamlObjectMapper.setSerializationInclusion(Include.NON_EMPTY);
        yamlObjectMapper.configure(MapperFeature.USE_GETTERS_AS_SETTERS, false);
        return yamlObjectMapper;
    }

    //    @Bean
    public YAMLFactory yamlFactory() {
        class CustomYAMLFactory extends YAMLFactory {

            private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

            @Override
            protected YAMLGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
                int feats = _yamlGeneratorFeatures;
                return yamlGenerator(ctxt, _generatorFeatures, feats, _objectCodec, out, _version);
            }
        }

        return new CustomYAMLFactory()
            .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
            .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, false)
            .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true)
            .configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false);
    }

    private YAMLGenerator yamlGenerator(
        IOContext ctxt,
        int jsonFeatures,
        int yamlFeatures,
        ObjectCodec codec,
        Writer out,
        org.yaml.snakeyaml.DumperOptions.Version version
    ) throws IOException {
        class MyYAMLGenerator extends YAMLGenerator {

            public MyYAMLGenerator(
                IOContext ctxt,
                int jsonFeatures,
                int yamlFeatures,
                ObjectCodec codec,
                Writer out,
                org.yaml.snakeyaml.DumperOptions.Version version
            ) throws IOException {
                super(ctxt, jsonFeatures, yamlFeatures, null, codec, out, version);
            }

            @Override
            protected DumperOptions buildDumperOptions(
                int jsonFeatures,
                int yamlFeatures,
                org.yaml.snakeyaml.DumperOptions.Version version
            ) {
                DumperOptions opt = super.buildDumperOptions(jsonFeatures, yamlFeatures, version);
                // override opts
                opt.setDefaultScalarStyle(ScalarStyle.LITERAL);
                opt.setDefaultFlowStyle(FlowStyle.BLOCK);
                opt.setIndicatorIndent(2);
                opt.setIndent(4);
                opt.setPrettyFlow(true);
                opt.setCanonical(false);
                return opt;
            }
        }

        return new MyYAMLGenerator(ctxt, jsonFeatures, yamlFeatures, codec, out, version);
    }

    /*
     * Session registry
     */

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        if (applicationUrl != null) {
            if (applicationUrl.startsWith("https")) {
                // use None as SameSite policy because we save requests in session for OIDC/SAML
                // TODO adopt LAX and resolve POST issues with session
                serializer.setSameSite("None");
                // we use None only with Secure=true otherwise it won't work
                // NOTE: this will break session on http requests
                serializer.setUseSecureCookie(true);
            } else {
                serializer.setSameSite("Lax");
            }
        }

        // config can override
        if (StringUtils.hasText(sessionCookieSameSite)) {
            serializer.setSameSite(sessionCookieSameSite);
        }

        if (sessionCookieSecure != null) {
            serializer.setUseSecureCookie(sessionCookieSecure.booleanValue());
        }

        return serializer;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /*
     * Wire persistence services bound to dataSource
     */

    @Bean
    public UserAccountService<OIDCUserAccount> oidcUserAccountService(
        OIDCUserAccountEntityRepository accountRepository,
        SubjectService subjectService
    ) {
        return new OIDCJpaUserAccountService(accountRepository, subjectService);
    }

    @Bean
    public UserAccountService<SamlUserAccount> samlUserAccountService(
        SamlUserAccountEntityRepository accountRepository,
        SubjectService subjectService
    ) {
        return new SamlJpaUserAccountService(accountRepository, subjectService);
    }

    @Bean
    public UserAccountService<InternalUserAccount> internalUserAccountService(
        InternalUserAccountEntityRepository accountRepository,
        SubjectService subjectService
    ) {
        return new InternalJpaUserAccountService(accountRepository, subjectService);
    }

    @Bean
    public InternalPasswordUserCredentialsService internalUserPasswordService(
        InternalUserPasswordRepository passwordRepository
    ) {
        return new InternalPasswordUserCredentialsService(passwordRepository);
    }

    @Bean
    public WebAuthnUserCredentialsService webAuthnCredentialsService(
        WebAuthnUserCredentialsEntityRepository credentialsRepository
    ) {
        return new WebAuthnUserCredentialsService(credentialsRepository);
    }

    @Bean
    public ConfigurableProviderEntityService<AttributeProviderEntity> attributeProviderEntityService(
        AttributeProviderEntityRepository attributeProviderRepository
    ) {
        return new ConfigurableProviderEntityService<>(attributeProviderRepository);
    }

    @Bean
    public ConfigurableProviderEntityService<IdentityProviderEntity> identityProviderEntityService(
        IdentityProviderEntityRepository identityProviderRepository
    ) {
        return new ConfigurableProviderEntityService<>(identityProviderRepository);
    }

    @Bean
    public ConfigurableProviderEntityService<TemplateProviderEntity> templateProviderEntityService(
        TemplateProviderEntityRepository templateProviderRepository
    ) {
        return new ConfigurableProviderEntityService<>(templateProviderRepository);
    }

    @Bean
    public AutoJdbcAttributeStore attributeStore() {
        return new AutoJdbcAttributeStore(dataSource);
    }

    @Bean(name = "scopeRegistry")
    public InMemoryScopeRegistry scopeRegistry(Collection<ScopeProvider> scopeProviders) {
        return new InMemoryScopeRegistry(scopeProviders);
    }

    @Bean(name = "extractorsRegistry")
    public ExtractorsRegistry extractorsRegistry(
        Collection<ScopeClaimsExtractorProvider> scopeExtractorsProviders,
        Collection<ResourceClaimsExtractorProvider> resourceExtractorsProviders
    ) {
        return new InMemoryExtractorsRegistry(scopeExtractorsProviders, resourceExtractorsProviders);
    }

    /*
     * TODO use a proper builder to obtain implementation
     */
    //    @Bean
    //    public ProviderConfigRepository<InternalAccountServiceConfig> internalServiceConfigRepository() {
    //        return new InMemoryProviderConfigRepository<InternalAccountServiceConfig>();
    //    }

    @Bean
    public ProviderConfigRepository<InternalIdentityProviderConfig> internalProviderConfigRepository() {
        return buildProviderConfigRepository(InternalIdentityProviderConfig.class);
    }

    @Bean
    public ProviderConfigRepository<PasswordIdentityProviderConfig> internalPasswordProviderConfigRepository() {
        return buildProviderConfigRepository(PasswordIdentityProviderConfig.class);
    }

    @Bean
    public ProviderConfigRepository<OIDCIdentityProviderConfig> oidcProviderConfigRepository() {
        return buildProviderConfigRepository(OIDCIdentityProviderConfig.class);
    }

    @Bean
    public ProviderConfigRepository<AppleIdentityProviderConfig> appleProviderConfigRepository() {
        return buildProviderConfigRepository(AppleIdentityProviderConfig.class);
    }

    @Bean
    public ProviderConfigRepository<SamlIdentityProviderConfig> samlProviderConfigRepository() {
        return buildProviderConfigRepository(SamlIdentityProviderConfig.class);
    }

    @Bean
    public ProviderConfigRepository<WebAuthnIdentityProviderConfig> webAuthnProviderConfigRepository() {
        return buildProviderConfigRepository(WebAuthnIdentityProviderConfig.class);
    }

    @Bean
    public ProviderConfigRepository<MapperAttributeProviderConfig> mapperProviderConfigRepository() {
        return buildProviderConfigRepository(MapperAttributeProviderConfig.class);
    }

    @Bean
    public ProviderConfigRepository<ScriptAttributeProviderConfig> scriptProviderConfigRepository() {
        return buildProviderConfigRepository(ScriptAttributeProviderConfig.class);
    }

    @Bean
    public ProviderConfigRepository<InternalAttributeProviderConfig> internalAttributeProviderConfigRepository() {
        return buildProviderConfigRepository(InternalAttributeProviderConfig.class);
    }

    @Bean
    public ProviderConfigRepository<WebhookAttributeProviderConfig> webhookAttributeProviderConfigRepository() {
        return buildProviderConfigRepository(WebhookAttributeProviderConfig.class);
    }

    //    @Bean
    //    public ProviderConfigRepository<InternalIdentityServiceConfig> internalIdentityServiceConfigRepository() {
    //        return new InMemoryProviderConfigRepository<InternalIdentityServiceConfig>();
    //    }
    //
    //    @Bean
    //    public ProviderConfigRepository<PasswordCredentialsServiceConfig> passwordCredentialsServiceConfigRepository() {
    //        return new InMemoryProviderConfigRepository<PasswordCredentialsServiceConfig>();
    //    }
    //
    //    @Bean
    //    public ProviderConfigRepository<WebAuthnCredentialsServiceConfig> webauthnCredentialsServiceConfigRepository() {
    //        return new InMemoryProviderConfigRepository<WebAuthnCredentialsServiceConfig>();
    //    }
    @Bean
    public ProviderConfigRepository<WebAuthnCredentialsServiceConfig> webauthnCredentialsServiceConfigRepository(
        ProviderConfigRepository<WebAuthnIdentityProviderConfig> externalRepository
    ) {
        return new WebAuthnConfigTranslatorRepository(externalRepository);
    }

    @Bean
    public ProviderConfigRepository<RealmTemplateProviderConfig> templateProviderConfigRepository() {
        return buildProviderConfigRepository(RealmTemplateProviderConfig.class);
    }

    private <U extends AbstractProviderConfig<?, ?>> ProviderConfigRepository<U> buildProviderConfigRepository(
        Class<U> clazz
    ) {
        if ("jdbc".equals(providerConfigRepository)) {
            return new AutoJDBCProviderConfigRepository<U>(dataSource, clazz);
        }

        return new InMemoryProviderConfigRepository<U>();
    }
}
