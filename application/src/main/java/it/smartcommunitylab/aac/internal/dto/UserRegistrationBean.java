/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.internal.dto;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import org.springframework.util.StringUtils;

/**
 * @author raman
 *
 */
@Valid
public class UserRegistrationBean {

    @NotEmpty
    @Email(message = "{validation.email}")
    private String email;

    @NotEmpty
    private String name;

    @NotEmpty
    private String surname;

    @Size(min = 5, message = "{validation.pwdlength}")
    private String password;

    @Size(min = 5, message = "{validation.pwdlength}")
    private String verifyPassword;

    private String lang;

    public UserRegistrationBean() {
        super();
    }

    public UserRegistrationBean(String email, String name, String surname) {
        super();
        this.email = email;
        this.name = name;
        this.surname = surname;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the surname
     */
    public String getSurname() {
        return surname;
    }

    /**
     * @param surname the surname to set
     */
    public void setSurname(String surname) {
        this.surname = surname;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public String getVerifyPassword() {
        return verifyPassword;
    }

    public void setVerifyPassword(String verifyPassword) {
        this.verifyPassword = verifyPassword;
    }

    @AssertTrue(message = "error.mismatch_passwords")
    private boolean isValid() {
        return password == null || (StringUtils.hasText(password) && password.equals(verifyPassword));
    }

    /**
     * @return the lang
     */
    public String getLang() {
        return lang;
    }

    /**
     * @param lang the lang to set
     */
    public void setLang(String lang) {
        this.lang = lang;
    }

    @Override
    public String toString() {
        return (
            "UserRegistrationBean [email=" +
            email +
            ", name=" +
            name +
            ", surname=" +
            surname +
            ", password=" +
            password +
            ", verifyPassword=" +
            verifyPassword +
            ", lang=" +
            lang +
            "]"
        );
    }
}
