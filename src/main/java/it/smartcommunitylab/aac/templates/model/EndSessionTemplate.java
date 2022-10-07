package it.smartcommunitylab.aac.templates.model;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;

public class EndSessionTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "endsession";
    private static final String[] KEYS = { "endsession.text" };

    public EndSessionTemplate(String realm) {
        super(SystemKeys.AUTHORITY_TEMPLATE, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }

}
