package it.smartcommunitylab.aac.templates.model;

import it.smartcommunitylab.aac.SystemKeys;
import java.util.Arrays;

public class LoginTemplate extends FixedTemplateModel {

    public static final String TEMPLATE = "login";
    private static final String[] KEYS = { "login.text", "login.register" };

    public LoginTemplate(String realm) {
        super(SystemKeys.AUTHORITY_TEMPLATE, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }
}
