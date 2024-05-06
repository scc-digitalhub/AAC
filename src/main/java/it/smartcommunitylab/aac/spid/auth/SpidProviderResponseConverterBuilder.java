package it.smartcommunitylab.aac.spid.auth;

import static it.smartcommunitylab.aac.spid.provider.SpidAuthenticationProvider.ACR_ATTRIBUTE;
import static it.smartcommunitylab.aac.spid.provider.SpidAuthenticationProvider.ISSUER_ATTRIBUTE;

import it.smartcommunitylab.aac.spid.model.SpidAuthnContext;
import it.smartcommunitylab.aac.spid.model.SpidError;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.util.CollectionUtils;

public class SpidProviderResponseConverterBuilder {

    public SpidProviderResponseConverterBuilder() {}

    public Converter<OpenSaml4AuthenticationProvider.ResponseToken, ? extends AbstractAuthenticationToken> build() {
        // leverage opensaml default response authentication converter, then expand with custom logic and behaviour
        Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication> defaultResponseConverter =
            OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter();
        return responseToken -> {
            Response response = responseToken.getResponse();
            Saml2Authentication auth = defaultResponseConverter.convert(responseToken);

            // extract extra information and add as attribute, then rebuild auth with new enriched attributes and default authority
            Map<String, List<Object>> attributes = new HashMap<>(
                ((Saml2AuthenticatedPrincipal) auth.getPrincipal()).getAttributes()
            );
            SpidAuthnContext authCtx = SpidProviderResponseValidatorBuilder.extractAcrValue(response);
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
}
