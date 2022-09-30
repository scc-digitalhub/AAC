package it.smartcommunitylab.aac.internal.templates;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;

public class InternalConfirmTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "confirmaccount";
    private static final String[] KEYS = { "confirmaccount.text", "confirmaccount_success.text" ,"confirmaccount_error.text"};

    public InternalConfirmTemplate(String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }

}
