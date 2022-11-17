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
        super(SystemKeys.AUTHORITY_INTERNAL, null, null, null);
    }

    public InternalEditableUserAccount(String provider, String realm, String userId, String uuid) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm, userId, uuid);
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
