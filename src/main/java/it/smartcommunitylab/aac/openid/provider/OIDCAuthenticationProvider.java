package it.smartcommunitylab.aac.openid.provider;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.OpenIdAttributesMapper;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticationException;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticationToken;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

public class OIDCAuthenticationProvider
        extends ExtendedAuthenticationProvider<OIDCUserAuthenticatedPrincipal, OIDCUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<OIDCUserAccount> accountService;
    private final OIDCIdentityProviderConfig config;
    private final String repositoryId;

    private final OidcAuthorizationCodeAuthenticationProvider oidcProvider;
    private final OAuth2LoginAuthenticationProvider oauthProvider;

    private String subAttributeName;

    protected String customMappingFunction;
    protected ScriptExecutionService executionService;
    protected final OpenIdAttributesMapper openidMapper;

    public OIDCAuthenticationProvider(String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            OIDCIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountService, config, realm);
    }

    public OIDCAuthenticationProvider(
            String authority, String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            OIDCIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.config = config;
        this.accountService = accountService;

        // repositoryId is always providerId, oidc isolates data per provider
        this.repositoryId = providerId;

        // let config set subject attribute name
        this.subAttributeName = config.getSubAttributeName();

        // build appropriate client auth request converter
        OAuth2AuthorizationCodeGrantRequestEntityConverter requestEntityConverter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();
        // private key jwt resolver, as per
        // https://tools.ietf.org/html/rfc7523#section-2.2
        if (ClientAuthenticationMethod.PRIVATE_KEY_JWT.equals(config.getClientAuthenticationMethod())) {
            // fetch key
            JWK jwk = config.getClientJWK();

            // build resolver only for this registration
            Function<ClientRegistration, JWK> jwkResolver = (clientRegistration) -> {
                if (providerId.equals(clientRegistration.getRegistrationId())) {
                    return jwk;
                }

                return null;
            };

            requestEntityConverter.addParametersConverter(
                    new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver));
        }

        // client secret jwt resolver, as per
        // https://tools.ietf.org/html/rfc7523#section-2.2
        if (ClientAuthenticationMethod.CLIENT_SECRET_JWT.equals(config.getClientAuthenticationMethod())) {
            // build key from secret as HmacSHA256
            SecretKeySpec secretKey = new SecretKeySpec(
                    config.getConfigMap().getClientSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            JWK jwk = new OctetSequenceKey.Builder(secretKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();

            // build resolver only for this registration
            Function<ClientRegistration, JWK> jwkResolver = (clientRegistration) -> {
                if (providerId.equals(clientRegistration.getRegistrationId())) {
                    return jwk;
                }

                return null;
            };

            requestEntityConverter.addParametersConverter(
                    new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver));
        }

        // we support only authCode login
        DefaultAuthorizationCodeTokenResponseClient accessTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
        accessTokenResponseClient.setRequestEntityConverter(requestEntityConverter);

        // we don't use the account repository to fetch user details,
        // use oidc userinfo to provide user details
        // TODO add jwt handling from id_token or access token
        this.oidcProvider = new OidcAuthorizationCodeAuthenticationProvider(accessTokenResponseClient,
                new OidcUserService());
        // oauth userinfo comes from oidc userinfo..
        this.oauthProvider = new OAuth2LoginAuthenticationProvider(accessTokenResponseClient,
                new DefaultOAuth2UserService());

        // use a custom authorities mapper to cleanup authorities spring injects
        // default impl translates the whole oauth response as an authority..
        this.oidcProvider.setAuthoritiesMapper(nullAuthoritiesMapper);
        this.oauthProvider.setAuthoritiesMapper(nullAuthoritiesMapper);

        // attribute mapper to extract email
        this.openidMapper = new OpenIdAttributesMapper();
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    public void setCustomMappingFunction(String customMappingFunction) {
        this.customMappingFunction = customMappingFunction;
    }

    @Override
    public Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        // extract registrationId and check if matches our providerId
        OAuth2LoginAuthenticationToken loginAuthenticationToken = (OAuth2LoginAuthenticationToken) authentication;
        String registrationId = loginAuthenticationToken.getClientRegistration().getRegistrationId();
        if (!getProvider().equals(registrationId)) {
            // this login is not for us, let others process it
            return null;
        }

        // TODO extract codeResponse + tokenResponse for audit
        String authorizationRequestUri = loginAuthenticationToken.getAuthorizationExchange().getAuthorizationRequest()
                .getAuthorizationRequestUri();
        String authorizationResponseUri = loginAuthenticationToken.getAuthorizationExchange().getAuthorizationResponse()
                .getRedirectUri();

        // delegate to oauth providers in sequence
        try {
            Authentication auth = oidcProvider.authenticate(authentication);
            if (auth == null) {
                auth = oauthProvider.authenticate(authentication);
            }

            if (auth != null) {
                // convert to our authToken and clear exchange information, those are not
                // serializable..
                OAuth2LoginAuthenticationToken authenticationToken = (OAuth2LoginAuthenticationToken) auth;
                // extract sub identifier
                String subject = authenticationToken.getPrincipal().getAttribute(subAttributeName);
                if (!StringUtils.hasText(subject)) {
                    throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request"));
                }

                // check if account is present and locked
                OIDCUserAccount account = accountService.findAccountById(repositoryId, subject);
                if (account != null && account.isLocked()) {
                    throw new OIDCAuthenticationException(new OAuth2Error("invalid_request"), "account not available",
                            authorizationRequestUri,
                            authorizationResponseUri, null, null);
                }

                auth = new OIDCAuthenticationToken(
                        subject,
                        authenticationToken.getPrincipal(),
                        authenticationToken.getAccessToken(),
                        authenticationToken.getRefreshToken(),
                        Collections.singleton(new SimpleGrantedAuthority(Config.R_USER)));
            }

            return auth;
        } catch (OAuth2AuthenticationException e) {
            throw new OIDCAuthenticationException(e.getError(), e.getMessage(), authorizationRequestUri,
                    authorizationResponseUri, null, null);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication != null && OAuth2LoginAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    protected OIDCUserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        // we need to unpack user and fetch properties from repo
        OAuth2User oauthDetails = (OAuth2User) principal;

        // upstream subject identifier
        String subject = oauthDetails.getAttribute(subAttributeName);

        // name is always available, is mapped via provider configuration
        String username = oauthDetails.getName();

        // we still don't have userId
        String userId = null;

        // rebuild details to clear authorities
        // by default they contain the response body, ie. the full accessToken +
        // everything else

        // bind principal to ourselves
        OIDCUserAuthenticatedPrincipal user = new OIDCUserAuthenticatedPrincipal(getAuthority(), getProvider(),
                getRealm(),
                userId, subject);
        user.setUsername(username);
        user.setPrincipal(oauthDetails);

        // custom attribute mapping
        if (executionService != null && StringUtils.hasText(customMappingFunction)) {
            try {
                // get all attributes from principal except jwt attrs
                // TODO handle all attributes not only strings.
                Map<String, Serializable> principalAttributes = user.getAttributes().entrySet().stream()
                        .filter(e -> !ArrayUtils.contains(OIDCIdentityProvider.JWT_ATTRIBUTES, e.getKey()))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
                // execute script
                Map<String, Serializable> customAttributes = executionService.executeFunction(
                        IdentityProvider.ATTRIBUTE_MAPPING_FUNCTION,
                        customMappingFunction, principalAttributes);

                // update map
                if (customAttributes != null) {
                    // replace map
                    principalAttributes = customAttributes.entrySet().stream()
                            .filter(e -> !ArrayUtils.contains(OIDCIdentityProvider.JWT_ATTRIBUTES, e.getKey()))
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
                    user.setAttributes(principalAttributes);
                }
            } catch (SystemException | InvalidDefinitionException ex) {
                logger.debug("error mapping principal attributes via script: " + ex.getMessage());
            }
        }

        // map attributes to openid set and flatten to string
        AttributeSet oidcAttributeSet = openidMapper.mapAttributes(user.getAttributes());
        Map<String, String> oidcAttributes = oidcAttributeSet.getAttributes()
                .stream()
                .collect(Collectors.toMap(
                        a -> a.getKey(),
                        a -> a.exportValue()));

        // fetch email when available
        String email = oidcAttributes.get(OpenIdAttributesSet.EMAIL);

        boolean defaultVerifiedStatus = config.getConfigMap().getTrustEmailAddress() != null
                ? config.getConfigMap().getTrustEmailAddress()
                : false;
        boolean emailVerified = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
                ? Boolean.parseBoolean(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
                : defaultVerifiedStatus;

        if (Boolean.TRUE.equals(config.getConfigMap().getAlwaysTrustEmailAddress())) {
            emailVerified = true;
        }

        // read username from attributes, mapper can replace it
        username = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.PREFERRED_USERNAME))
                ? oidcAttributes.get(OpenIdAttributesSet.PREFERRED_USERNAME)
                : user.getUsername();

        // update principal
        user.setUsername(username);
        user.setEmail(email);
        user.setEmailVerified(emailVerified);

        return user;
    }

    @Override
    protected Instant expiresAt(Authentication auth) {
        // if enabled bind session duration to token expiration
        if (Boolean.TRUE.equals(config.getConfigMap().getRespectTokenExpiration())) {
            // build expiration from tokens
            OIDCAuthenticationToken token = (OIDCAuthenticationToken) auth;
            OAuth2User user = token.getPrincipal();
            if (user instanceof OidcUser) {
                // check for id token
                Instant exp = ((OidcUser) user).getExpiresAt();
                if (exp != null) {
                    return exp;
                }
            }

            OAuth2AccessToken accessToken = token.getAccessToken();
            if (accessToken != null) {
                return accessToken.getExpiresAt();
            }
        }

        return null;

    }

    private final GrantedAuthoritiesMapper nullAuthoritiesMapper = (authorities -> Collections.emptyList());

}
