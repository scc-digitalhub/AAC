package it.smartcommunitylab.aac.internal.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractLoginProvider;

public class InternalLoginProvider extends AbstractLoginProvider {

    public InternalLoginProvider(String provider, String realm, String name) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm, name);
    }

    private String registrationUrl;
    private String resetUrl;
    private String formUrl;

    public String getRegistrationUrl() {
        return registrationUrl;
    }

    public void setRegistrationUrl(String registrationUrl) {
        this.registrationUrl = registrationUrl;
    }

    public String getResetUrl() {
        return resetUrl;
    }

    public void setResetUrl(String resetUrl) {
        this.resetUrl = resetUrl;
    }

    public String getFormUrl() {
        return formUrl;
    }

    public void setFormUrl(String formUrl) {
        this.formUrl = formUrl;
    }
}
