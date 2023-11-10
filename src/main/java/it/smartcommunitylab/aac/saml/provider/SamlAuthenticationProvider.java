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

package it.smartcommunitylab.aac.saml.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProvider;
import it.smartcommunitylab.aac.saml.SamlKeys;
import it.smartcommunitylab.aac.saml.auth.SamlAuthenticationException;
import it.smartcommunitylab.aac.saml.auth.SamlAuthenticationToken;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.saml.model.SamlUserAuthenticatedPrincipal;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class SamlAuthenticationProvider
    extends ExtendedAuthenticationProvider<SamlUserAuthenticatedPrincipal, SamlUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SUBJECT_ATTRIBUTE = "subject";
    private final UserAccountService<SamlUserAccount> accountService;
    private final SamlIdentityProviderConfig config;
    private final String repositoryId;

    private final String usernameAttributeName;

    // let config set subject attribute name
    private final String subAttributeName;

    private final OpenSaml4AuthenticationProvider openSamlProvider;

    private String customMappingFunction;
    private ScriptExecutionService executionService;

    public SamlAuthenticationProvider(
        String providerId,
        UserAccountService<SamlUserAccount> accountService,
        SamlIdentityProviderConfig config,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_SAML, providerId, accountService, config, realm);
    }

    public SamlAuthenticationProvider(
        String authority,
        String providerId,
        UserAccountService<SamlUserAccount> accountService,
        SamlIdentityProviderConfig config,
        String realm
    ) {
        super(authority, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.config = config;
        this.accountService = accountService;

        // repositoryId is always providerId, saml isolates data per provider
        this.repositoryId = providerId;

        this.usernameAttributeName =
            StringUtils.hasText(config.getConfigMap().getUserNameAttributeName())
                ? config.getConfigMap().getUserNameAttributeName()
                : SUBJECT_ATTRIBUTE;

        this.subAttributeName = config.getSubAttributeName();

        // build a default saml provider with a clock skew of 5 minutes
        openSamlProvider = new OpenSaml4AuthenticationProvider();
        //        openSamlProvider.setAssertionValidator(OpenSamlAuthenticationProvider
        //                .createDefaultAssertionValidator(assertionToken -> {
        //                    Map<String, Object> params = new HashMap<>();
        //                    params.put(SAML2AssertionValidationParameters.CLOCK_SKEW, Duration.ofMinutes(5).toMillis());
        //                    return new ValidationContext(params);
        //                }));

        // TODO rework response converter to extract additional assertions as attributes
        //
        //        Converter<ResponseToken, Saml2Authentication> authenticationConverter = OpenSamlAuthenticationProvider
        //                .createDefaultResponseAuthenticationConverter();
        //        openSamlProvider.setResponseAuthenticationConverter((responseToken) -> {
        //            Response response = responseToken.getResponse();
        ////            Saml2AuthenticationToken token = responseToken.getToken();
        //            Assertion assertion = CollectionUtils.firstElement(response.getAssertions());
        //            Saml2Authentication auth = authenticationConverter.convert(responseToken);
        //
        //            // integrate assertions as attributes
        //            Map<String, List<Object>> attributes = new HashMap<>();
        //            attributes.putAll(((DefaultSaml2AuthenticatedPrincipal) auth.getPrincipal()).getAttributes());
        //            if (assertion.getIssuer() != null) {
        //                attributes.put("issuer", Collections.singletonList(assertion.getIssuer().getValue()));
        //            }
        //            if (assertion.getIssueInstant() != null) {
        //                attributes.put("issueInstant", Collections.singletonList(assertion.getIssueInstant().toString()));
        //            }
        //
        //            attributes.put(SUBJECT_ATTRIBUTE, Collections.singletonList(assertion.getSubject().getNameID().getValue()));
        //
        //            return new SamlAuthentication(new DefaultSaml2AuthenticatedPrincipal(auth.getName(), attributes),
        //                    responseToken, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        //        });

        // use a custom authorities mapper to cleanup authorities
        // replaced with converter
        //        openSamlProvider.setAuthoritiesMapper(nullAuthoritiesMapper);
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    public void setCustomMappingFunction(String customMappingFunction) {
        this.customMappingFunction = customMappingFunction;
    }

    @Override
    public Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        // extract registrationId and check if matches our providerid
        Saml2AuthenticationToken loginAuthenticationToken = (Saml2AuthenticationToken) authentication;
        String registrationId = loginAuthenticationToken.getRelyingPartyRegistration().getRegistrationId();
        if (!getProvider().equals(registrationId)) {
            // this login is not for us, let others process it
            return null;
        }

        // TODO fetch also saml2Request
        String saml2Response = loginAuthenticationToken.getSaml2Response();

        // delegate to openSaml, and leverage default converter
        try {
            Authentication auth = openSamlProvider.authenticate(authentication);
            //            SamlAuthentication auth = (SamlAuthentication) token;
            if (auth != null) {
                // convert to our authToken
                Saml2Authentication authToken = (Saml2Authentication) auth;
                // extract sub identifier
                String subject = authToken.getName();
                if (!StringUtils.hasText(subject)) {
                    throw new SamlAuthenticationException(
                        new Saml2Error(Saml2ErrorCodes.USERNAME_NOT_FOUND, "username_not_found")
                    );
                }

                // check if account is present and locked
                SamlUserAccount account = accountService.findAccountById(repositoryId, subject);
                if (account != null && account.isLocked()) {
                    throw new SamlAuthenticationException(
                        new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "account_unavailable"),
                        "account not available",
                        null,
                        saml2Response
                    );
                }

                auth =
                    new SamlAuthenticationToken(
                        subject,
                        (Saml2AuthenticatedPrincipal) authToken.getPrincipal(),
                        saml2Response,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            }

            // TODO wrap response token and erase credentials etc
            return auth;
        } catch (Saml2AuthenticationException e) {
            throw new SamlAuthenticationException(e.getSaml2Error(), e.getMessage(), null, saml2Response);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication != null && Saml2AuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    protected SamlUserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        // we need to unpack token and fetch properties from repo
        // note we expect default behavior, if provider has a converter this will break
        Saml2AuthenticatedPrincipal samlDetails = (Saml2AuthenticatedPrincipal) principal;

        // subjectId is optionally evaluated from an attribute defined in saml provider configurations
        String subjectId = StringUtils.hasText(subAttributeName)
            ? samlDetails.getFirstAttribute(subAttributeName)
            : samlDetails.getName();
        if (subjectId == null) {
            logger.error("Failed to evaluate or obtain the subjectId in the the Saml2AuthenticatedPrincipal");
            return null; // fail authentication
        }

        // name is always available, by default is subjectId
        String name = samlDetails.getName();

        // username mapping, default name always set
        String username = StringUtils.hasText(samlDetails.getFirstAttribute(usernameAttributeName))
            ? samlDetails.getFirstAttribute(usernameAttributeName)
            : name;

        // we still don't have userId
        String userId = null;

        // bind principal to ourselves
        SamlUserAuthenticatedPrincipal user = new SamlUserAuthenticatedPrincipal(
            getProvider(),
            getRealm(),
            userId,
            subjectId
        );
        user.setUsername(username);
        user.setPrincipal(samlDetails);

        // custom attribute mapping
        if (executionService != null && StringUtils.hasText(customMappingFunction)) {
            try {
                // get all attributes from principal except jwt attrs
                // TODO handle all attributes not only strings.
                Map<String, Serializable> principalAttributes = user
                    .getAttributes()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue() != null)
                    .filter(e -> !SamlKeys.SAML_ATTRIBUTES.contains(e.getKey()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
                // execute script
                Map<String, Serializable> customAttributes = executionService.executeFunction(
                    IdentityProvider.ATTRIBUTE_MAPPING_FUNCTION,
                    customMappingFunction,
                    principalAttributes
                );

                // update map
                if (customAttributes != null) {
                    // replace map
                    principalAttributes =
                        customAttributes
                            .entrySet()
                            .stream()
                            .filter(e -> e.getValue() != null)
                            .filter(e -> !SamlKeys.SAML_ATTRIBUTES.contains(e.getKey()))
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
                    user.setAttributes(principalAttributes);
                }
            } catch (SystemException | InvalidDefinitionException ex) {
                logger.debug("error mapping principal attributes via script: " + ex.getMessage());
            }
        }

        // re-read attributes as-is, transform to strings
        // TODO evaluate using a custom mapper to given profile
        Map<String, String> samlAttributes = user
            .getAttributes()
            .entrySet()
            .stream()
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));

        // fetch email when available
        String email = samlAttributes.get("email");
        boolean defaultVerifiedStatus = config.getConfigMap().getTrustEmailAddress() != null
            ? config.getConfigMap().getTrustEmailAddress()
            : false;
        boolean emailVerified = StringUtils.hasText(samlAttributes.get("emailVerified"))
            ? Boolean.parseBoolean(samlAttributes.get("emailVerified"))
            : defaultVerifiedStatus;

        if (Boolean.TRUE.equals(config.getConfigMap().getAlwaysTrustEmailAddress())) {
            emailVerified = true;
        }

        // read username from attributes, mapper can replace it
        username =
            StringUtils.hasText(samlAttributes.get(usernameAttributeName))
                ? samlAttributes.get(usernameAttributeName)
                : user.getUsername();

        // update principal
        user.setUsername(username);
        user.setEmail(email);
        user.setEmailVerified(emailVerified);

        return user;
    }
    //    private final GrantedAuthoritiesMapper nullAuthoritiesMapper = (authorities -> Collections.emptyList());

}
