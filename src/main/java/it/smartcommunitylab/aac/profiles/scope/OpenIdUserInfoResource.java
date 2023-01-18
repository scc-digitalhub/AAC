package it.smartcommunitylab.aac.profiles.scope;

import org.springframework.security.oauth2.core.oidc.StandardClaimNames;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.model.BooleanClaimDefinition;
import it.smartcommunitylab.aac.claims.model.DateClaimDefinition;
import it.smartcommunitylab.aac.claims.model.StringClaimDefinition;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;

public class OpenIdUserInfoResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = "openid.userinfo";

    public OpenIdUserInfoResource(String realm, String baseUrl) {
        super(SystemKeys.AUTHORITY_OIDC, realm, baseUrl, RESOURCE_ID);

        // statically register scopes
        // TODO evaluate making configurable
        setScopes(
                new OpenIdDefaultScope(realm),
                new OpenIdEmailScope(realm),
                new OpenIdAddressScope(realm),
                new OpenIdPhoneScope(realm));

        // set definitions for every standard claim
        setClaims(
                new StringClaimDefinition(StandardClaimNames.NAME),
                new StringClaimDefinition(StandardClaimNames.NICKNAME),
                new StringClaimDefinition(StandardClaimNames.GIVEN_NAME),
                new StringClaimDefinition(StandardClaimNames.FAMILY_NAME),
                new StringClaimDefinition(StandardClaimNames.PREFERRED_USERNAME),
                new DateClaimDefinition(StandardClaimNames.BIRTHDATE),
                new StringClaimDefinition(StandardClaimNames.EMAIL),
                new BooleanClaimDefinition(StandardClaimNames.EMAIL_VERIFIED),
                new StringClaimDefinition(StandardClaimNames.PHONE_NUMBER),
                new BooleanClaimDefinition(StandardClaimNames.PHONE_NUMBER_VERIFIED),
                new StringClaimDefinition(StandardClaimNames.ADDRESS));
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "OpenId user info";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Access user openid profile: basic, email, address, phone";
//    }
}
