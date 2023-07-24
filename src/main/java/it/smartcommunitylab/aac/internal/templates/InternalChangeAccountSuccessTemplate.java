package it.smartcommunitylab.aac.internal.templates;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;
import java.util.Arrays;

public class InternalChangeAccountSuccessTemplate extends FixedTemplateModel {

    public static final String TEMPLATE = "changeaccount_success";
    private static final String[] KEYS = { "changeaccount_success.text" };

    public InternalChangeAccountSuccessTemplate(String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }
}
