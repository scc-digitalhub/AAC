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

package it.smartcommunitylab.aac.eidas.provider;

import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.eidas.model.EIDASUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.model.SamlUserAccount;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticationException;
import it.smartcommunitylab.aac.spid.model.SpidAttribute;
import it.smartcommunitylab.aac.spid.model.SpidAttributeConsumingService;
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.provider.SpidAuthenticationProvider;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;

/*
 * Authentication against the eIDAS node.
 *
 * The whole SPID pipeline is inherited: openSaml provider, assertion/response validators, response
 * converter, account lock check and the custom hook functions. Here we only rebrand the result:
 * the authority is eidas and the principal is an EIDASUserAuthenticatedPrincipal.
 */
public class EIDASAuthenticationProvider extends SpidAuthenticationProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Pattern ATTRIBUTE_NAME = Pattern.compile("<(?:[\\w.-]+:)?Attribute\\s[^>]*?\\bName=\"([^\"]+)\"");

    // kept for diagnostics: what the SP metadata asks for, per attribute consuming service
    private final EIDASIdentityProviderConfig config;

    public EIDASAuthenticationProvider(
        String providerId,
        UserAccountService<SamlUserAccount> accountService,
        EIDASIdentityProviderConfig config,
        String realm
    ) {
        super(providerId, accountService, config, realm);
        this.config = config;
    }

    /*
     * SpidError.translate() keeps only the error code: the Saml2Error description, the only thing
     * telling WHICH check failed, is dropped before reaching the logs. The SPID stack stays
     * untouched, so we log it here, on the way out.
     *
     * The error shown to the user is left as the SPID stack built it. Mapping the SAML status of an
     * eIDAS node onto a SPID anomaly would mean guessing: eIDAS SAML Message Format v1.4.1 (2.4.2)
     * only requires a Status with at least one StatusCode, says nothing about second level codes or
     * StatusMessage, and eIDAS Interoperability Architecture v1.4.1 (5) defers error handling to
     * SAML Core. Which sub status a node sends for, say, a refused consent is its own choice, so
     * there is nothing to map against until the node we talk to documents its own.
     */
    @Override
    protected Authentication doAuthenticate(Authentication authentication) {
        try {
            return super.doAuthenticate(authentication);
        } catch (AuthenticationException e) {
            String detail = e.getMessage();
            if (e instanceof SpidAuthenticationException) {
                SpidAuthenticationException se = (SpidAuthenticationException) e;
                logger.error(
                    "eidas authentication failed for provider {}: error {} ({}) - {}",
                    getProvider(),
                    se.getError().getErrorCode(),
                    se.getError().getMessage(),
                    detail
                );
                // on the generic codes the message carries no detail (it is the error code itself),
                // and the response is the only place left where to see what the node sent
                if (se.getError().getValue() >= 1000) {
                    logger.debug("eidas response was {}", se.getSaml2Response());
                }
                // "missing requested attributes" (1013) names no attribute: say which ones, so that a
                // configuration asking for attributes the node does not release is diagnosed, not guessed
                if (detail != null && detail.contains("missing requested attributes")) {
                    logRequestedVersusObtained(authentication);
                }
            } else {
                logger.error("eidas authentication failed for provider {}: {}", getProvider(), detail);
            }
            throw e;
        }
    }

    /*
     * The attributes of every attribute consuming service in the SP metadata against the SPID-named
     * attributes found in the Response (the eIDAS URI names are not SPID attributes and are not
     * counted, exactly as the SPID validator does not count them).
     */
    private void logRequestedVersusObtained(Authentication authentication) {
        try {
            Set<String> obtained = new TreeSet<>();
            if (authentication instanceof Saml2AuthenticationToken) {
                String response = ((Saml2AuthenticationToken) authentication).getSaml2Response();
                Matcher m = ATTRIBUTE_NAME.matcher(response != null ? response : "");
                while (m.find()) {
                    SpidAttribute a = SpidAttribute.parse(m.group(1));
                    if (a != null) {
                        obtained.add(a.getValue());
                    }
                }
            }
            for (SpidAttributeConsumingService acs : config.getAttributeConsumingServices()) {
                Set<String> requested = new TreeSet<>();
                Set<String> missing = new TreeSet<>();
                if (acs.getAttributes() != null) {
                    acs.getAttributes().forEach(a -> requested.add(a.getValue()));
                    requested.stream().filter(a -> !obtained.contains(a)).forEach(missing::add);
                }
                logger.warn(
                    "eidas provider {} attribute consuming service {}: requested {}, obtained {}, missing {}",
                    getProvider(),
                    acs.getIndex(),
                    requested,
                    obtained,
                    missing
                );
            }
        } catch (RuntimeException ex) {
            logger.debug("could not compare requested and obtained attributes: {}", ex.getMessage());
        }
    }

    @Override
    protected SpidUserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        // let SPID do the parsing, the attribute extraction and the script hooks
        SpidUserAuthenticatedPrincipal spidPrincipal = super.createUserPrincipal(principal);
        if (spidPrincipal == null) {
            return null;
        }

        // rebuild as an eidas principal
        EIDASUserAuthenticatedPrincipal user = new EIDASUserAuthenticatedPrincipal(
            getAuthority(),
            getProvider(),
            getRealm(),
            spidPrincipal.getUserId(),
            spidPrincipal.getSubjectId()
        );
        user.setPrincipal(spidPrincipal.getPrincipal());
        user.setUsername(spidPrincipal.getUsername());
        user.setEmailAddress(spidPrincipal.getEmailAddress());
        user.setIdp(spidPrincipal.getIdp());
        user.setSpidCode(spidPrincipal.getSpidCode());
        user.setAttributes(spidPrincipal.getAttributes());

        return user;
    }
}
