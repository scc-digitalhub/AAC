package it.smartcommunitylab.aac.saml;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class SamlKeys {

    static final String[] SAML_ATTRIBUTES_VALUES = { "subject", "issuer", "issueInstant" };

    static final String[] ACCOUNT_ATTRIBUTES_VALUES = { "username", "name", "email", "locale" };

    public static final Set<String> SAML_ATTRIBUTES;
    public static final Set<String> ACCOUNT_ATTRIBUTES;

    static {
        SAML_ATTRIBUTES = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(SAML_ATTRIBUTES_VALUES)));
        ACCOUNT_ATTRIBUTES = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(ACCOUNT_ATTRIBUTES_VALUES)));
    }
}
