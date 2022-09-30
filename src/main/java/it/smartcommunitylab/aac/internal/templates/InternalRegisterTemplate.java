package it.smartcommunitylab.aac.internal.templates;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;

public class InternalRegisterTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "registeraccount";
    private static final String[] KEYS = { "registeraccount.text", "registeraccount_success.text" };

    public InternalRegisterTemplate(String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }

}
