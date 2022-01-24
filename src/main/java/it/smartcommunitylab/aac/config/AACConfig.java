package it.smartcommunitylab.aac.config;

import java.io.IOException;
import java.io.Writer;

import javax.sql.DataSource;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.MapperAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProviderConfig;
import it.smartcommunitylab.aac.claims.ClaimsService;
import it.smartcommunitylab.aac.claims.DefaultClaimsService;
import it.smartcommunitylab.aac.claims.ExtractorsRegistry;
import it.smartcommunitylab.aac.claims.LocalGraalExecutionService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.ExtendedUserAuthenticationManager;
import it.smartcommunitylab.aac.core.auth.DefaultSecurityContextAuthenticationHelper;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwarePathUriBuilder;
import it.smartcommunitylab.aac.core.provider.UserTranslator;
import it.smartcommunitylab.aac.core.service.CoreUserTranslator;
import it.smartcommunitylab.aac.core.service.InMemoryProviderRepository;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.auth.SamlRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;

/*
 * AAC core config
 */
@Configuration
@Order(5)
public class AACConfig {

    @Value("${application.url}")
    private String applicationUrl;

    /*
     * Core aac should be bootstrapped before services, security etc
     */

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AuthorityManager authorityManager;

    /*
     * provider manager depends on authorities + static config + datasource
     */

    // @Autowired
    // private ProviderManager providerManager;

    @Bean
    @ConfigurationProperties(prefix = "providers")
    public ProvidersProperties globalProviders() {
        return new ProvidersProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "attributesets")
    public AttributeSetsProperties systemAttributeSets() {
        return new AttributeSetsProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spid")
    public SpidProperties spidProperties() {
        return new SpidProperties();
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper;
    }

    @Bean
    public ObjectMapper yamlObjectMapper() {
        // YAMLFactory factory = new YAMLFactory()
        // .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
        // .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);

        YAMLFactory factory = yamlFactory();
        ObjectMapper yamlObjectMapper = new ObjectMapper(factory);
        yamlObjectMapper.setSerializationInclusion(Include.NON_EMPTY);
        return yamlObjectMapper;
    }

    @Bean
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
                super(ctxt, jsonFeatures, yamlFeatures, codec, out, version);
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
     * authManager depends on provider + userService
     */
    @Autowired
    private SubjectService subjectService;

    @Autowired
    private UserEntityService userService;

    @Bean
    public ExtendedUserAuthenticationManager extendedAuthenticationManager() throws Exception {
        return new ExtendedUserAuthenticationManager(authorityManager, userService, subjectService);
    }

    @Bean
    public AuthenticationHelper authenticationHelper() {
        return new DefaultSecurityContextAuthenticationHelper();
    }

    /*
     * we need all beans covering authorities here, otherwise we won't be able to
     * build the authmanager (it depends on providerManager -> authorityManager)
     * 
     * TODO fix configuration, expose setter on authManager
     */
    @Bean
    public OIDCClientRegistrationRepository clientRegistrationRepository() {
        return new OIDCClientRegistrationRepository();
    }

    @Bean
    @Qualifier("samlRelyingPartyRegistrationRepository")
    public SamlRelyingPartyRegistrationRepository samlRelyingPartyRegistrationRepository() {
        return new SamlRelyingPartyRegistrationRepository();
    }

    @Bean
    @Qualifier("spidRelyingPartyRegistrationRepository")
    public SamlRelyingPartyRegistrationRepository spidRelyingPartyRegistrationRepository() {
        return new SamlRelyingPartyRegistrationRepository();
    }

    @Bean
    public InMemoryProviderRepository<InternalIdentityProviderConfig> internalProviderConfigRepository() {
        return new InMemoryProviderRepository<InternalIdentityProviderConfig>();
    }

    @Bean
    public InMemoryProviderRepository<WebAuthnIdentityProviderConfig> webauthnProviderConfigRepository() {
        return new InMemoryProviderRepository<WebAuthnIdentityProviderConfig>();
    }

    @Bean
    public InMemoryProviderRepository<OIDCIdentityProviderConfig> oidcProviderConfigRepository() {
        return new InMemoryProviderRepository<OIDCIdentityProviderConfig>();
    }

    @Bean
    public InMemoryProviderRepository<SamlIdentityProviderConfig> samlProviderConfigRepository() {
        return new InMemoryProviderRepository<SamlIdentityProviderConfig>();
    }

    @Bean
    public InMemoryProviderRepository<SpidIdentityProviderConfig> spidProviderConfigRepository() {
        return new InMemoryProviderRepository<SpidIdentityProviderConfig>();
    }

    @Bean
    public InMemoryProviderRepository<MapperAttributeProviderConfig> mapperProviderConfigRepository() {
        return new InMemoryProviderRepository<MapperAttributeProviderConfig>();
    }

    @Bean
    public InMemoryProviderRepository<ScriptAttributeProviderConfig> scriptProviderConfigRepository() {
        return new InMemoryProviderRepository<ScriptAttributeProviderConfig>();
    }

    @Bean
    public InMemoryProviderRepository<InternalAttributeProviderConfig> internalAttributeProviderConfigRepository() {
        return new InMemoryProviderRepository<InternalAttributeProviderConfig>();
    }

    @Bean
    public InMemoryProviderRepository<WebhookAttributeProviderConfig> webhookAttributeProviderConfigRepository() {
        return new InMemoryProviderRepository<WebhookAttributeProviderConfig>();
    }
    /*
     * initialize the execution service here and then build claims service
     */

    @Bean
    public LocalGraalExecutionService localGraalExecutionService() {
        return new LocalGraalExecutionService();
    }

    @Bean
    public ClaimsService claimsService(ExtractorsRegistry extractorsRegistry,
            ScriptExecutionService executionService,
            UserService userService) {
        DefaultClaimsService service = new DefaultClaimsService(extractorsRegistry);
        service.setExecutionService(executionService);
        service.setUserService(userService);
        return service;

    }

    /*
     * Cross realm user translator
     */
    @Bean
    public UserTranslator userTranslator() {
        return new CoreUserTranslator();
    }

    /*
     * Entrypoint
     */
    // TODO make sure all filters use this bean to build urls..
    @Bean
    public RealmAwarePathUriBuilder realmUriBuilder() {
        return new RealmAwarePathUriBuilder(applicationUrl);
    }

    // @Autowired
    // private UserRepository userRepository;
    //
    //// @Autowired
    //// private ClientDetailsRepository clientDetailsRepository;
    //
    // @Autowired
    // private DataSource dataSource;

    // @Autowired
    // private TokenStore tokenStore;
    //
    // @Autowired
    // private OIDCTokenEnhancer tokenEnhancer;
    //
    // @Autowired
    // private AACJwtTokenConverter tokenConverter;
    //
    // /*
    // * OAuth
    // */
    // // TODO split specifics to dedicated configurer
    //
    // @Bean
    // public AutoJdbcTokenStore getTokenStore() throws PropertyVetoException {
    // return new AutoJdbcTokenStore(dataSource);
    // }
    //
    // @Bean
    // public JdbcApprovalStore getApprovalStore() throws PropertyVetoException {
    // return new JdbcApprovalStore(dataSource);
    // }
    //
    // @Bean
    // @Primary
    // public JdbcClientDetailsService getClientDetails() throws
    // PropertyVetoException {
    // JdbcClientDetailsService bean = new AACJDBCClientDetailsService(dataSource);
    // bean.setRowMapper(getClientDetailsRowMapper());
    // return bean;
    // }

    // @Bean
    // @Primary
    // public UserDetailsService getInternalUserDetailsService() {
    // return new InternalUserDetailsRepo();
    // }
    //
    // @Bean
    // public ClientDetailsRowMapper getClientDetailsRowMapper() {
    // return new ClientDetailsRowMapper(userRepository);
    // }
    //
    // @Bean
    // @Primary
    // public OAuth2ClientDetailsProvider getOAuth2ClientDetailsProvider() throws
    // PropertyVetoException {
    // return new OAuth2ClientDetailsProviderImpl(clientDetailsRepository);
    // }

    // @Bean
    // public FilterRegistrationBean
    // oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
    // FilterRegistrationBean registration = new FilterRegistrationBean();
    // registration.setFilter(filter);
    // registration.setOrder(-100);
    // return registration;
    // }

    // @Bean
    // public CachedServiceScopeServices getResourceStorage() {
    // return new CachedServiceScopeServices();
    // }

    // @Bean("appTokenServices")
    // public NonRemovingTokenServices getTokenServices() throws
    // PropertyVetoException {
    // NonRemovingTokenServices bean = new NonRemovingTokenServices();
    // bean.setTokenStore(tokenStore);
    // bean.setSupportRefreshToken(true);
    // bean.setReuseRefreshToken(true);
    // bean.setAccessTokenValiditySeconds(accessTokenValidity);
    // bean.setRefreshTokenValiditySeconds(refreshTokenValidity);
    // bean.setClientDetailsService(getClientDetails());
    // if (oauth2UseJwt) {
    // bean.setTokenEnhancer(new AACTokenEnhancer(tokenEnhancer, tokenConverter));
    // } else {
    // bean.setTokenEnhancer(new AACTokenEnhancer(tokenEnhancer));
    // }
    //
    // return bean;
    // }

    // /*
    // * Authorities handlers
    // */
    //
    // @Bean
    // public IdentitySource getIdentitySource() {
    // return new FileEmailIdentitySource();
    // }
    //
    // @Bean
    // public AuthorityHandlerContainer getAuthorityHandlerContainer() {
    // Map<String, AuthorityHandler> map = Maps.newTreeMap();
    // map.put(Config.IDP_INTERNAL, getInternalHandler());
    // FBAuthorityHandler fh = new FBAuthorityHandler();
    // map.put("facebook", fh);
    // AuthorityHandlerContainer bean = new AuthorityHandlerContainer(map);
    // return bean;
    // }
    //
    // @Bean
    // public DefaultAuthorityHandler getDefaultHandler() {
    // return new DefaultAuthorityHandler();
    // }
    //
    // @Bean
    // public InternalAuthorityHandler getInternalHandler() {
    // return new InternalAuthorityHandler();
    // }

}
