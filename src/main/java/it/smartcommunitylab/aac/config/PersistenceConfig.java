package it.smartcommunitylab.aac.config;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.MapperAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.claims.ExtractorsRegistry;
import it.smartcommunitylab.aac.claims.InMemoryExtractorsRegistry;
import it.smartcommunitylab.aac.claims.ResourceClaimsExtractorProvider;
import it.smartcommunitylab.aac.claims.ScopeClaimsExtractorProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.service.InMemoryProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.service.OIDCUserAccountService;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.password.service.InternalUserPasswordService;
import it.smartcommunitylab.aac.saml.auth.SamlRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.service.SamlUserAccountService;
import it.smartcommunitylab.aac.scope.InMemoryScopeRegistry;
import it.smartcommunitylab.aac.scope.ScopeProvider;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsServiceConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnConfigTranslatorRepository;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserCredentialsService;

@Configuration
@Order(2)
public class PersistenceConfig {

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
        return yamlObjectMapper;
    }

//    @Bean
    public YAMLFactory yamlFactory() {
        class CustomYAMLFactory extends YAMLFactory {
            private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

            @Override
            protected YAMLGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
                int feats = _yamlGeneratorFeatures;
                return yamlGenerator(ctxt, _generatorFeatures, feats,
                        _objectCodec, out, _version);
            }
        }

        return new CustomYAMLFactory()
                .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
                .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, false)
                .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true);
    }

    private YAMLGenerator yamlGenerator(IOContext ctxt, int jsonFeatures, int yamlFeatures,
            ObjectCodec codec, Writer out,
            org.yaml.snakeyaml.DumperOptions.Version version) throws IOException {

        class MyYAMLGenerator extends YAMLGenerator {

            public MyYAMLGenerator(IOContext ctxt, int jsonFeatures, int yamlFeatures,
                    ObjectCodec codec, Writer out, org.yaml.snakeyaml.DumperOptions.Version version)
                    throws IOException {
                super(ctxt, jsonFeatures, yamlFeatures, null, codec, out, version);
            }

            @Override
            protected DumperOptions buildDumperOptions(int jsonFeatures, int yamlFeatures,
                    org.yaml.snakeyaml.DumperOptions.Version version) {
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

        return new MyYAMLGenerator(ctxt, jsonFeatures, yamlFeatures, codec,
                out, version);
    }

    /*
     * Wire persistence services bound to dataSource
     */

    @Bean
    public UserAccountService<OIDCUserAccount> oidcUserAccountService(OIDCUserAccountRepository accountRepository,
            SubjectService subjectService) {
        return new OIDCUserAccountService(accountRepository, subjectService);
    }

    @Bean
    public UserAccountService<SamlUserAccount> samlUserAccountService(SamlUserAccountRepository accountRepository,
            SubjectService subjectService) {
        return new SamlUserAccountService(accountRepository, subjectService);
    }

    @Bean
    public UserAccountService<InternalUserAccount> internalUserAccountService(
            InternalUserAccountRepository accountRepository, SubjectService subjectService) {
        return new InternalUserAccountService(accountRepository, subjectService);
    }

    @Bean
    public InternalUserPasswordService internalUserPasswordService(InternalUserPasswordRepository passwordRepository) {
        return new InternalUserPasswordService(passwordRepository);
    }

    @Bean
    public WebAuthnUserCredentialsService webAuthnCredentialsService(
            WebAuthnUserCredentialsRepository credentialsRepository) {
        return new WebAuthnUserCredentialsService(credentialsRepository);
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
    public ExtractorsRegistry extractorsRegistry(Collection<ScopeClaimsExtractorProvider> scopeExtractorsProviders,
            Collection<ResourceClaimsExtractorProvider> resourceExtractorsProviders) {
        return new InMemoryExtractorsRegistry(scopeExtractorsProviders, resourceExtractorsProviders);
    }

    /*
     * we need all beans covering authorities here, otherwise we won't be able to
     * build the authmanager (it depends on providerManager -> authorityManager)
     * 
     * TODO fix configuration, expose setter on authManager
     */
    @Bean
    @Qualifier("oidcClientRegistrationRepository")
    @Primary
    public OIDCClientRegistrationRepository oidcClientRegistrationRepository() {
        return new OIDCClientRegistrationRepository();
    }

    @Bean
    @Qualifier("appleClientRegistrationRepository")
    public OIDCClientRegistrationRepository appleClientRegistrationRepository() {
        return new OIDCClientRegistrationRepository();
    }

    @Bean
    @Qualifier("samlRelyingPartyRegistrationRepository")
    public SamlRelyingPartyRegistrationRepository samlRelyingPartyRegistrationRepository() {
        return new SamlRelyingPartyRegistrationRepository();
    }

    /*
     * TODO make configurable via properties and use builder to obtain
     * implementation
     */
//    @Bean
//    public ProviderConfigRepository<InternalAccountServiceConfig> internalServiceConfigRepository() {
//        return new InMemoryProviderConfigRepository<InternalAccountServiceConfig>();
//    }

    @Bean
    public ProviderConfigRepository<InternalIdentityProviderConfig> internalProviderConfigRepository() {
        return new InMemoryProviderConfigRepository<InternalIdentityProviderConfig>();
    }

    @Bean
    public ProviderConfigRepository<PasswordIdentityProviderConfig> internalPasswordProviderConfigRepository() {
        return new InMemoryProviderConfigRepository<PasswordIdentityProviderConfig>();
    }

    @Bean
    public ProviderConfigRepository<OIDCIdentityProviderConfig> oidcProviderConfigRepository() {
        return new InMemoryProviderConfigRepository<OIDCIdentityProviderConfig>();
    }

    @Bean
    public ProviderConfigRepository<AppleIdentityProviderConfig> appleProviderConfigRepository() {
        return new InMemoryProviderConfigRepository<AppleIdentityProviderConfig>();
    }

    @Bean
    public ProviderConfigRepository<SamlIdentityProviderConfig> samlProviderConfigRepository() {
        return new InMemoryProviderConfigRepository<SamlIdentityProviderConfig>();
    }

    @Bean
    public ProviderConfigRepository<WebAuthnIdentityProviderConfig> webAuthnProviderConfigRepository() {
        return new InMemoryProviderConfigRepository<WebAuthnIdentityProviderConfig>();
    }

    @Bean
    public ProviderConfigRepository<MapperAttributeProviderConfig> mapperProviderConfigRepository() {
        return new InMemoryProviderConfigRepository<MapperAttributeProviderConfig>();
    }

    @Bean
    public ProviderConfigRepository<ScriptAttributeProviderConfig> scriptProviderConfigRepository() {
        return new InMemoryProviderConfigRepository<ScriptAttributeProviderConfig>();
    }

    @Bean
    public ProviderConfigRepository<InternalAttributeProviderConfig> internalAttributeProviderConfigRepository() {
        return new InMemoryProviderConfigRepository<InternalAttributeProviderConfig>();
    }

    @Bean
    public ProviderConfigRepository<WebhookAttributeProviderConfig> webhookAttributeProviderConfigRepository() {
        return new InMemoryProviderConfigRepository<WebhookAttributeProviderConfig>();
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
            ProviderConfigRepository<WebAuthnIdentityProviderConfig> externalRepository) {
        return new WebAuthnConfigTranslatorRepository(externalRepository);
    }
}
