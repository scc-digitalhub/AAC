package it.smartcommunitylab.aac.password.templates;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;

public class PasswordResetTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "resetpwd";
    private static final String[] KEYS = { "resetpwd.text", "resetpwd_success.text" };

    public PasswordResetTemplate(String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }

}
