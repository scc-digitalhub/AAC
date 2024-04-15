/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.password.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;

@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordPolicy implements Serializable {

    private int passwordMinLength = 5;
    private int passwordMaxLength = 12;
    private boolean passwordRequireAlpha = true;
    private boolean passwordRequireUppercaseAlpha = false;
    private boolean passwordRequireNumber = true;
    private boolean passwordRequireSpecial = false;
    private boolean passwordSupportWhitespace = false;

    public int getPasswordMinLength() {
        return passwordMinLength;
    }

    public void setPasswordMinLength(int passwordMinLength) {
        this.passwordMinLength = passwordMinLength;
    }

    public int getPasswordMaxLength() {
        return passwordMaxLength;
    }

    public void setPasswordMaxLength(int passwordMaxLength) {
        this.passwordMaxLength = passwordMaxLength;
    }

    public boolean isPasswordRequireAlpha() {
        return passwordRequireAlpha;
    }

    public void setPasswordRequireAlpha(boolean passwordRequireAlpha) {
        this.passwordRequireAlpha = passwordRequireAlpha;
    }

    public boolean isPasswordRequireUppercaseAlpha() {
        return passwordRequireUppercaseAlpha;
    }

    public void setPasswordRequireUppercaseAlpha(boolean passwordRequireUppercaseAlpha) {
        this.passwordRequireUppercaseAlpha = passwordRequireUppercaseAlpha;
    }

    public boolean isPasswordRequireNumber() {
        return passwordRequireNumber;
    }

    public void setPasswordRequireNumber(boolean passwordRequireNumber) {
        this.passwordRequireNumber = passwordRequireNumber;
    }

    public boolean isPasswordRequireSpecial() {
        return passwordRequireSpecial;
    }

    public void setPasswordRequireSpecial(boolean passwordRequireSpecial) {
        this.passwordRequireSpecial = passwordRequireSpecial;
    }

    public boolean isPasswordSupportWhitespace() {
        return passwordSupportWhitespace;
    }

    public void setPasswordSupportWhitespace(boolean passwordSupportWhitespace) {
        this.passwordSupportWhitespace = passwordSupportWhitespace;
    }

    public String getPasswordPattern() {
        // translate policy to input pattern
        StringBuilder sb = new StringBuilder();
        if (isPasswordRequireAlpha()) {
            // require alpha means any, we add pattern for [a-z]
            // TODO fix pattern
            sb.append("(?=.*[a-z])");
        }
        if (isPasswordRequireUppercaseAlpha()) {
            sb.append("(?=.*[A-Z])");
        }
        if (isPasswordRequireNumber()) {
            sb.append("(?=.*\\d)");
        }
        if (isPasswordRequireSpecial()) {
            // TODO
        }

        // add length
        sb.append(".{").append(getPasswordMinLength()).append(",").append(getPasswordMaxLength()).append("}");

        return sb.toString();
    }

    @Override
    // TODO replace with proper description supporting i18n
    public String toString() {
        return (
            "PasswordPolicy [passwordMinLength=" +
            passwordMinLength +
            ", passwordMaxLength=" +
            passwordMaxLength +
            ", passwordRequireAlpha=" +
            passwordRequireAlpha +
            ", passwordRequireNumber=" +
            passwordRequireNumber +
            ", passwordRequireSpecial=" +
            passwordRequireSpecial +
            ", passwordSupportWhitespace=" +
            passwordSupportWhitespace +
            "]"
        );
    }
}
