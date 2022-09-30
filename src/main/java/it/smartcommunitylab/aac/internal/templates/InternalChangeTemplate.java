package it.smartcommunitylab.aac.internal.templates;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.templates.model.FixedTemplateModel;

public class InternalChangeTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "change";
    private static final String[] KEYS = { "change.text" };

    public InternalChangeTemplate(String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }

}
