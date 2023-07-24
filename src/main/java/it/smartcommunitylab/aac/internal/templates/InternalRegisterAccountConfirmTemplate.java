package it.smartcommunitylab.aac.internal.templates;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;
import java.util.Arrays;

public class InternalRegisterAccountConfirmTemplate extends FixedTemplateModel {

    public static final String TEMPLATE = "registeraccount_confirm";
    //TODO add views for success and error
    //    private static final String[] KEYS = { "confirmaccount.text", "confirmaccount_success.text" ,"confirmaccount_error.text"};
    private static final String[] KEYS = { "registeraccount_confirm.text" };

    public InternalRegisterAccountConfirmTemplate(String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }
}
