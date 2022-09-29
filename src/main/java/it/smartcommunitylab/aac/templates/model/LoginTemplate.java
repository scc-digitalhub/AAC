package it.smartcommunitylab.aac.templates.model;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;

public class LoginTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "login";
    private static final String[] KEYS = { "login.text", "login.register" };

    public LoginTemplate(String realm) {
        super(SystemKeys.AUTHORITY_TEMPLATE, realm, TEMPLATE, Arrays.asList(KEYS));
    }

}
