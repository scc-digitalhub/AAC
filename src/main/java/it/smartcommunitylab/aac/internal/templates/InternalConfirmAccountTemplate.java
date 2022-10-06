package it.smartcommunitylab.aac.internal.templates;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;

public class InternalConfirmAccountTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "confirmaccount";
    //TODO add views for success and error
//    private static final String[] KEYS = { "confirmaccount.text", "confirmaccount_success.text" ,"confirmaccount_error.text"};
    private static final String[] KEYS = { "confirmaccount.text"};

    public InternalConfirmAccountTemplate(String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }

}
