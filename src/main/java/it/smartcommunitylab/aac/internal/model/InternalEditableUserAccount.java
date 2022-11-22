package it.smartcommunitylab.aac.internal.model;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractEditableAccount;

@Valid
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalEditableUserAccount extends AbstractEditableAccount {
    private static final long serialVersionUID = SystemKeys.AAC_INTERNAL_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_CREDENTIALS + SystemKeys.ID_SEPARATOR
            + SystemKeys.AUTHORITY_INTERNAL;

    @NotBlank
    private String username;

    // attributes
    @NotEmpty
    @Email(message = "{validation.email}")
    private String email;

    @Size(min = 2, max = 70)
    private String name;

    @Size(min = 2, max = 70)
    private String surname;

    private String lang;

    protected InternalEditableUserAccount() {
        super(SystemKeys.AUTHORITY_INTERNAL, null, null);
    }

    public InternalEditableUserAccount(String provider, String uuid) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, uuid);
    }

    public InternalEditableUserAccount(String provider, String realm, String userId, String uuid) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, uuid);
        setRealm(realm);
        setUserId(userId);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getAccountId() {
        return username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

}
