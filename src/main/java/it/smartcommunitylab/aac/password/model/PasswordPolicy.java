package it.smartcommunitylab.aac.password.model;

public class PasswordPolicy {
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

    @Override
    // TODO replace with proper description supporting i18n
    public String toString() {
        return "PasswordPolicy [passwordMinLength=" + passwordMinLength + ", passwordMaxLength=" + passwordMaxLength
                + ", passwordRequireAlpha=" + passwordRequireAlpha + ", passwordRequireNumber=" + passwordRequireNumber
                + ", passwordRequireSpecial=" + passwordRequireSpecial + ", passwordSupportWhitespace="
                + passwordSupportWhitespace + "]";
    }

}
