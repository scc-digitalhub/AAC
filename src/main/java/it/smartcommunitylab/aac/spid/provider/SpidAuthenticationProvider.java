/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProvider;
import it.smartcommunitylab.aac.saml.auth.SamlAuthenticationException;
import it.smartcommunitylab.aac.saml.auth.SamlAuthenticationToken;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticationException;
import it.smartcommunitylab.aac.spid.auth.SpidResponseValidator;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.model.SpidError;
import it.smartcommunitylab.aac.spid.model.SpidUserAttribute;
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class SpidAuthenticationProvider
    extends ExtendedAuthenticationProvider<SpidUserAuthenticatedPrincipal, SpidUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final SpidUserAttribute SUBJECT_ATTRIBUTE = SpidUserAttribute.SUBJECT;
    private static final String ACR_ATTRIBUTE = "authnContextClassRef";
    private static final String ISSUER_ATTRIBUTE = "spidIssuer";
    private final UserAccountService<SpidUserAccount> accountService;
    private final Set<String> registrationIds;
    private final SpidUserAttribute subjectAttribute;
    private final SpidUserAttribute usernameAttribute;
    private final SpidResponseValidator spidValidator;
    private final String accountRepositoryId;

    private final OpenSaml4AuthenticationProvider openSamlProvider;
    private ScriptExecutionService executionService;
    private String customMappingFunction;
    protected String customAuthFunction;

    public SpidAuthenticationProvider(
        String providerId,
        UserAccountService<SpidUserAccount> accountService,
        SpidIdentityProviderConfig config,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_SPID, providerId, accountService, config, realm);
    }

    public SpidAuthenticationProvider(
        String authority,
        String providerId,
        UserAccountService<SpidUserAccount> accountService,
        SpidIdentityProviderConfig config,
        String realm
    ) {
        super(authority, providerId, realm);
        this.accountService = accountService;
        this.registrationIds = config.getRelyingPartyRegistrationIds();
        this.subjectAttribute = config.getSubAttributeName();
        this.usernameAttribute = config.getUsernameAttributeName();

        this.spidValidator = new SpidResponseValidator();
        this.openSamlProvider = new OpenSaml4AuthenticationProvider();
        this.openSamlProvider.setAssertionValidator(buildProviderAssertionValidator());
        this.openSamlProvider.setResponseValidator(buildResponseValidator());
        this.openSamlProvider.setResponseAuthenticationConverter(buildProviderResponseConverter());

        this.accountRepositoryId = providerId;
    }

    @Override
    protected Authentication doAuthenticate(Authentication authentication) {
        Saml2AuthenticationToken loginAuthenticationToken = (Saml2AuthenticationToken) authentication;
        String registrationId = loginAuthenticationToken.getRelyingPartyRegistration().getRegistrationId();
        if (!registrationIds.contains(registrationId)) {
            // this login is not for us, let others process it
            return null;
        }

        String serializedResponse = loginAuthenticationToken.getSaml2Response();
        try {
            Authentication auth = openSamlProvider.authenticate(authentication);
            if (auth != null) {
                // TODO: here Saml does the following (0) checks that the token has a subject, (1) checks that account isn't locked and (2) inserts a default role "ROLE_USER" - is this required? Btw this uses the (currently unused) accountService
                // convert to local token, after checking that the authentication is not associated to a locked account
                Saml2Authentication authSamlToken = (Saml2Authentication) auth;
                // extract sub identifier
                String subject = authSamlToken.getName();
                if (!StringUtils.hasText(subject)) {
                    throw new SamlAuthenticationException(
                        new Saml2Error(Saml2ErrorCodes.USERNAME_NOT_FOUND, "username_not_found")
                    );
                }
                // check if account is locked
                SpidUserAccount account = accountService.findAccountById(accountRepositoryId, subject);
                if (account != null && account.isLocked()) {
                    throw new SamlAuthenticationException(
                        new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "account_unavailable"),
                        "account not available",
                        null,
                        serializedResponse
                    );
                }
                auth =
                    new SamlAuthenticationToken(
                        subject,
                        (Saml2AuthenticatedPrincipal) authSamlToken.getPrincipal(),
                        serializedResponse,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            }
            return auth;
        } catch (Saml2AuthenticationException e) {
            // check if wrapped spid ex
            if (e.getCause() instanceof SpidAuthenticationException) {
                SpidError err = ((SpidAuthenticationException) e.getCause()).getError();
                throw new SpidAuthenticationException(err, null, serializedResponse);
            }

            throw new SpidAuthenticationException(e, null, serializedResponse);
        }
    }

    @Override
    protected SpidUserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        // we need to unpack token and fetch properties from repo
        // note we expect default behavior, if provider has a converter this will break
        Saml2AuthenticatedPrincipal samlDetails = (Saml2AuthenticatedPrincipal) principal;

        String subjectId = evaluateSubjectIdFromPrincipal(samlDetails);
        if (subjectId == null) {
            logger.error("Failed to evaluate or obtain the subjectId in the the Saml2AuthenticatedPrincipal");
            return null; // fail authentication
        }
        // A. create empty user with no attributes
        SpidUserAuthenticatedPrincipal user = new SpidUserAuthenticatedPrincipal(
            getProvider(),
            getRealm(),
            null, // userId is evaluated later from attributes
            subjectId
        );
        // username must exists: let choose from config, fallback to principal name
        String username = StringUtils.hasText(evaluateSubjectIdFromPrincipal(samlDetails))
            ? evaluateUsernameFromPrincipal(samlDetails)
            : samlDetails.getName();
        user.setUsername(username);
        user.setPrincipal(samlDetails);

        // update with eventual information from spid provider
        if (samlDetails.getAttributes().containsKey(SpidAttribute.SPID_CODE.getValue())) {
            String spidCode = (String) samlDetails
                .getAttributes()
                .get(SpidAttribute.SPID_CODE.getValue())
                .stream()
                .findFirst()
                .orElse(null);
            if (StringUtils.hasText(spidCode)) {
                user.setSpidCode(spidCode);
            }
        }
        if (samlDetails.getAttributes().containsKey(SpidAttribute.EMAIL.getValue())) {
            String email = (String) samlDetails
                .getAttributes()
                .get(SpidAttribute.EMAIL.getValue())
                .stream()
                .findFirst()
                .orElse(null);
            if (StringUtils.hasText(email)) {
                user.setEmailAddress(email);
            }
        }
        if (samlDetails.getAttributes().containsKey(ISSUER_ATTRIBUTE)) {
            String issuerIdp = (String) samlDetails
                .getAttributes()
                .get(ISSUER_ATTRIBUTE)
                .stream()
                .findFirst()
                .orElse(null);
            if (StringUtils.hasText(issuerIdp)) {
                user.setIdp(issuerIdp);
            }
        }

        HashMap<String, Serializable> principalAttributes = new HashMap<>();
        for (SpidAttribute attr : SpidAttribute.values()) {
            if (samlDetails.getAttributes().get(attr.getValue()) == null) {
                continue;
            }
            Object value = samlDetails.getAttributes().get(attr.getValue()).stream().findFirst().orElse(null);
            if (value != null) {
                principalAttributes.put(attr.getValue(), (Serializable) value);
            }
        }

        // custom attributes and authorization scripts
        if (executionService != null) {
            // get all attributes from principal
            // TODO handle all attributes not only strings.
            user
                .getAttributes()
                .entrySet()
                .stream()
                .filter(e -> e.getValue() != null)
                .forEach(e -> principalAttributes.put(e.getKey(), e.getValue()));

            //TODO build context with relevant info
            HashMap<String, Serializable> contextAttributes = new HashMap<>();
            contextAttributes.put("timestamp", Instant.now().getEpochSecond());
            contextAttributes.put("errors", new ArrayList<Serializable>());

            // evaluate custom authorization function
            if (StringUtils.hasText(customAuthFunction)) {
                try {
                    // execute script
                    Boolean authResult = executionService.executeFunction(
                        IdentityProvider.AUTHORIZATION_FUNCTION,
                        customAuthFunction,
                        Boolean.class,
                        principalAttributes,
                        contextAttributes
                    );

                    if (authResult != null) {
                        if (authResult.booleanValue() == false) {
                            // TODO: SpidException?  Non c'è un equivalente di unauthorized, però
                            throw new SamlAuthenticationException(
                                new Saml2Error(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR, "unauthorized")
                            );
                        }
                    }
                } catch (SystemException | InvalidDefinitionException ex) {
                    logger.debug("error executing authorize function via script: " + ex.getMessage());
                }
            }

            // evaluate custom attribute mapping function
            if (StringUtils.hasText(customMappingFunction)) {
                try {
                    // execute script
                    Map<String, Serializable> customAttributes = executionService.executeFunction(
                        IdentityProvider.ATTRIBUTE_MAPPING_FUNCTION,
                        customMappingFunction,
                        principalAttributes
                    );

                    // update map
                    if (customAttributes != null) {
                        // replace attributes
                        customAttributes
                            .entrySet()
                            .stream()
                            .filter(e -> e.getValue() != null)
                            // each spid attribute is authoritative and cannot be overwritten or re-evaluated
                            .filter(e -> SpidAttribute.contains(e.getKey()))
                            .forEach(e -> principalAttributes.put(e.getKey(), e.getValue()));
                        //                        user.setAttributes(principalAttributes); // TODO: da chiarire con Matteo
                    }
                } catch (SystemException | InvalidDefinitionException ex) {
                    logger.debug("error mapping principal attributes via script: " + ex.getMessage());
                }
            }
        }

        if (!principalAttributes.isEmpty()) {
            user.setAttributes(principalAttributes);
        }

        return user;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication != null && Saml2AuthenticationToken.class.isAssignableFrom(authentication);
    }

    private Converter<OpenSaml4AuthenticationProvider.AssertionToken, Saml2ResponseValidatorResult> buildProviderAssertionValidator() {
        // leverage opensaml default validator, then expand with custom logic and behaviour
        Converter<OpenSaml4AuthenticationProvider.AssertionToken, Saml2ResponseValidatorResult> defaultValidator =
            OpenSaml4AuthenticationProvider.createDefaultAssertionValidator();

        return assertionToken -> {
            // call default
            Saml2ResponseValidatorResult result = defaultValidator.convert(assertionToken);

            // TODO: valuta i codici di errore: attualmente sono una reference al test
            // assertions must be signed; note that signature validity is performed by default validator; here we check signature existence
            Assertion assertion = assertionToken.getAssertion();
            if (assertion.getSignature() == null) {
                return result.concat(new Saml2Error("SPID_ERROR_004", "missing assertion signature"));
            }

            // IssueInstant attribute must exists and be non trivial
            if (assertion.getIssueInstant() == null) {
                return result.concat(new Saml2Error("SPID_ERROR_037", "missing IssueInstant attribute"));
            }

            if (
                assertion.getSubject().getNameID() == null ||
                assertion.getSubject().getNameID().getNameQualifier() == null
            ) {
                return result.concat(new Saml2Error("SPID_ERROR_048", "missing NameQualifier attribute in NameId"));
            }

            if (!isSubjectConfirmationValid(assertion)) {
                return result.concat(new Saml2Error("SPID_ERROR_51", "missing SubjectConfirmation"));
            }
            return result;
        };
    }

    private Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2ResponseValidatorResult> buildResponseValidator() {
        // leaverage default validator
        Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2ResponseValidatorResult> defaultValidator =
            OpenSaml4AuthenticationProvider.createDefaultResponseValidator();
        return responseToken -> {
            Saml2ResponseValidatorResult result = defaultValidator.convert(responseToken);
            // TODO: verifica come scrivere CODICI ERRORE SPID
            // version must exists and must be 2.0 as per https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html#response
            Response response = responseToken.getResponse();
            if (response.getVersion() == null || !isVersion20(response)) {
                return result.concat(new Saml2Error("SPID_ERROR_010", "missing or wrong response version"));
            }

            // Issue Instant must be present, be non null, have correct format
            Instant issueInstant = response.getIssueInstant();
            if (issueInstant == null) {
                return result.concat(new Saml2Error("SPID_ERROR_011", "missing or undefined issue instant attribute"));
            }
            if (!isIssueInstantFormatValid(response)) {
                return result.concat(new Saml2Error("SPID_ERROR_012", "invalid issue instant format"));
            }

            // InResponseTo attribute must exists an d be nontrivial
            if (!StringUtils.hasText(response.getInResponseTo())) {
                return result.concat(new Saml2Error("SPID_ERROR_017", "missing or empty InResponseTo attribute"));
            }
            // Destination attribute must exists and be nontrivial
            if (!StringUtils.hasText(response.getDestination())) {
                return result.concat(new Saml2Error("SPID_ERROR_019", "missing or empty Destination attribute"));
            }

            // Issuer Format attribute must be entity
            if (!isIssuerFormatEntity(response)) {
                return result.concat(new Saml2Error("SPID_ERROR_030", "wrong or missing Issuer Format Attribute"));
            }

            return result;
        };
    }

    private boolean isIssueInstantFormatValid(Response response) {
        if (response.getDOM() == null || response.getDOM().getAttribute("IssueInstant").isEmpty()) {
            return false;
        }
        //        String issueInstant = response.getDOM().getAttribute("IssueInstant");
        //        try {
        //            LocalDate.parse(issueInstant, DateTimeFormatter.ISO_DATE);
        //        } catch (DateTimeParseException ex) {
        //            return false;
        //        }
        // TODO: rivedere: non chiaro quale sia il pattern da verificare causa documentazione inconsistente con validatore
        return true;
    }

    private boolean isIssuerFormatEntity(Response response) {
        if (response.getIssuer() == null || !StringUtils.hasText(response.getIssuer().getFormat())) {
            return false;
        }
        return response.getIssuer().getFormat().equals(NameIDType.ENTITY);
    }

    private boolean isVersion20(Response response) {
        return (response.getVersion().getMajorVersion() == 2 && response.getVersion().getMinorVersion() == 0);
    }

    private boolean isSubjectConfirmationValid(Assertion assertion) {
        if (assertion.getSubject().getSubjectConfirmations() == null) {
            return false;
        }
        if (assertion.getSubject().getSubjectConfirmations().isEmpty()) {
            return false;
        }
        SubjectConfirmation confirmation = assertion
            .getSubject()
            .getSubjectConfirmations()
            .stream()
            .findFirst()
            .orElse(null);
        if (confirmation == null) {
            return false;
        }
        return confirmation.getSubjectConfirmationData() != null;
    }

    private Converter<OpenSaml4AuthenticationProvider.ResponseToken, ? extends AbstractAuthenticationToken> buildProviderResponseConverter() {
        // leverage opensaml default response authentication converter, then expand with custom logic and behaviour
        Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication> defaultResponseConverter =
            OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter();
        return responseToken -> {
            Response response = responseToken.getResponse();
            Saml2Authentication auth = defaultResponseConverter.convert(responseToken);

            // TODO: rivedere la posizione del validator: alcuni controllo sono stati spostati su openSamlProvider.setResponseValidator(…) ma potrebbe non essere un approccio idoneo
            // validate as required by SPID
            spidValidator.validateResponse(responseToken);

            // extract extra information and add as attribute, then rebuild auth with new enriched attributes and default authority
            Map<String, List<Object>> attributes = new HashMap<>(
                ((Saml2AuthenticatedPrincipal) auth.getPrincipal()).getAttributes()
            );
            SpidAuthnContext authCtx = extractAcrValue(response);
            if (authCtx != null) {
                attributes.put(ACR_ATTRIBUTE, Collections.singletonList((Object) (authCtx.getValue())));
            }

            String issuer = extractIssuer(response);
            if (issuer != null) {
                attributes.put(ISSUER_ATTRIBUTE, Collections.singletonList(issuer));
            }
            // rebuild auth
            DefaultSaml2AuthenticatedPrincipal principal = new DefaultSaml2AuthenticatedPrincipal(
                auth.getName(),
                attributes
            );
            if (auth.getPrincipal() instanceof DefaultSaml2AuthenticatedPrincipal) {
                principal =
                    new DefaultSaml2AuthenticatedPrincipal(
                        auth.getName(),
                        attributes,
                        ((DefaultSaml2AuthenticatedPrincipal) auth.getPrincipal()).getSessionIndexes()
                    );
            }

            return new Saml2Authentication(
                principal,
                auth.getSaml2Response(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        };
    }

    /*
     * extractAcrValue parse and returns the ACR from a SPID SAML response.
     * If no ACR is successfully parser, null value is returned.
     */
    private static @Nullable SpidAuthnContext extractAcrValue(Response response) {
        Assertion assertion = CollectionUtils.firstElement(response.getAssertions());
        if (assertion == null) {
            return null;
        }

        AuthnContext authnContext = assertion
            .getAuthnStatements()
            .stream()
            .filter(a -> a.getAuthnContext() != null)
            .findFirst()
            .map(a -> a.getAuthnContext())
            .orElse(null);

        if (authnContext == null) {
            return null;
        }

        String acrValue = authnContext.getAuthnContextClassRef().getURI();
        return SpidAuthnContext.parse(acrValue);
    }

    /*
     * extractIssuer parse and returns the identity provider from a SPID SAML
     * response. If not identity provider is successfully parser, null value
     * is returned.
     */
    private static @Nullable String extractIssuer(Response response) {
        Assertion assertion = CollectionUtils.firstElement(response.getAssertions());
        if (assertion == null || assertion.getIssuer() == null) {
            return null;
        }

        return assertion.getIssuer().getValue();
    }

    private String evaluateSubjectIdFromPrincipal(Saml2AuthenticatedPrincipal principal) {
        return subjectAttribute != null
            ? principal.getFirstAttribute(subjectAttribute.getValue())
            : principal.getName();
    }

    private String evaluateUsernameFromPrincipal(Saml2AuthenticatedPrincipal principal) {
        return usernameAttribute != null
            ? principal.getFirstAttribute(usernameAttribute.getValue())
            : principal.getName();
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    public void setCustomMappingFunction(String customMappingFunction) {
        this.customMappingFunction = customMappingFunction;
    }

    public void setCustomAuthFunction(String customAuthFunction) {
        this.customAuthFunction = customAuthFunction;
    }
}
