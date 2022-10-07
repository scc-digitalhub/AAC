package it.smartcommunitylab.aac.openid;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;

public class OIDCKeys {

    static final String[] ACCOUNT_ATTRIBUTES_VALUES = {
            "username",
            OpenIdAttributesSet.NAME,
            OpenIdAttributesSet.FAMILY_NAME,
            OpenIdAttributesSet.GIVEN_NAME,
            OpenIdAttributesSet.PREFERRED_USERNAME,
            OpenIdAttributesSet.EMAIL,
            OpenIdAttributesSet.EMAIL_VERIFIED,
            OpenIdAttributesSet.PICTURE,
            OpenIdAttributesSet.LOCALE
    };

    static final String[] JWT_ATTRIBUTES_VALUES = {
            IdTokenClaimNames.ACR,
            IdTokenClaimNames.AMR,
            IdTokenClaimNames.AT_HASH,
            IdTokenClaimNames.AUD,
            IdTokenClaimNames.AUTH_TIME,
            IdTokenClaimNames.AZP,
            IdTokenClaimNames.C_HASH,
            IdTokenClaimNames.EXP,
            IdTokenClaimNames.IAT,
            IdTokenClaimNames.ISS,
            IdTokenClaimNames.NONCE,
            IdTokenClaimNames.SUB
    };

    public static final Set<String> JWT_ATTRIBUTES;
    public static final Set<String> ACCOUNT_ATTRIBUTES;

    static {
        JWT_ATTRIBUTES = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(JWT_ATTRIBUTES_VALUES)));
        ACCOUNT_ATTRIBUTES = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(ACCOUNT_ATTRIBUTES_VALUES)));

    }
}
