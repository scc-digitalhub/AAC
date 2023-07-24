package it.smartcommunitylab.aac.templates.model;

import it.smartcommunitylab.aac.SystemKeys;
import java.util.Arrays;

public class FooterTemplate extends FixedTemplateModel {

    public static final String TEMPLATE = "footer";
    private static final String[] KEYS = { "footer.text" };

    public FooterTemplate(String realm) {
        super(SystemKeys.AUTHORITY_TEMPLATE, realm, null, TEMPLATE, Arrays.asList(KEYS));
    }
}
