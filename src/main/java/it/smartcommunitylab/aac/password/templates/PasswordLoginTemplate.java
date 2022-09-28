package it.smartcommunitylab.aac.password.templates;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;
import it.smartcommunitylab.aac.templates.model.TemplateModel;

public class PasswordLoginTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "login";
    private static final String[] KEYS = { "label.login_pwd_reset" };

    public PasswordLoginTemplate(String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, realm, TEMPLATE, Arrays.asList(KEYS));
    }

    public PasswordLoginTemplate(String realm, TemplateModel model) {
        super(SystemKeys.AUTHORITY_PASSWORD, realm, TEMPLATE, Arrays.asList(KEYS));
        this.setLanguage(model.getLanguage());
        this.setContent(getContent());
    }

}
