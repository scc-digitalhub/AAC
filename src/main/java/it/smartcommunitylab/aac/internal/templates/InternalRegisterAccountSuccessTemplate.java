package it.smartcommunitylab.aac.internal.templates;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;

public class InternalRegisterAccountSuccessTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "registeraccount_success";
    private static final String[] KEYS = { "registeraccount_success.text" };

    public InternalRegisterAccountSuccessTemplate(String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }

}
