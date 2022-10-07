package it.smartcommunitylab.aac.password.templates;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;

public class PasswordPolicyTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "pwdpolicy";
    private static final String[] KEYS = { "pwdpolicy" };

    public PasswordPolicyTemplate(String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }

}
