package it.smartcommunitylab.aac.password.templates;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;
import java.util.Arrays;

public class PasswordChangeSuccessTemplate extends FixedTemplateModel {

    public static final String TEMPLATE = "changepwd_success";
    private static final String[] KEYS = { "changepwd_success.text" };

    public PasswordChangeSuccessTemplate(String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }
}
