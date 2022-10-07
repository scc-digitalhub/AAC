package it.smartcommunitylab.aac.templates.model;

import java.util.Arrays;

import it.smartcommunitylab.aac.SystemKeys;

public class UserApprovalTemplate extends FixedTemplateModel {
    public static final String TEMPLATE = "user-approval";
    private static final String[] KEYS = { "user-approval.text" };

    public UserApprovalTemplate(String realm) {
        super(SystemKeys.AUTHORITY_TEMPLATE, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }

}
