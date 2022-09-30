package it.smartcommunitylab.aac.password.templates;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;

public class PasswordLoginTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "loginpwd";
    private static final String[] KEYS = { "loginpwd.text" };

    public PasswordLoginTemplate(String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }

}
