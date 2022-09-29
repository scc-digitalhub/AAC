package it.smartcommunitylab.aac.templates.model;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;

public class UserApprovalTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "userapproval";
    private static final String[] KEYS = { "userapproval.text" };

    public UserApprovalTemplate(String realm) {
        super(SystemKeys.AUTHORITY_TEMPLATE, realm, TEMPLATE, Arrays.asList(KEYS));
    }

}
