package it.smartcommunitylab.aac.password.templates;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;

public class PasswordChangeTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "changepwd";
    private static final String[] KEYS = { "changepwd.text" };

    public PasswordChangeTemplate(String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }

}
