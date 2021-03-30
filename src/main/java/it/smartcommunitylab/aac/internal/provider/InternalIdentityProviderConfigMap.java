package it.smartcommunitylab.aac.internal.provider;

public class InternalIdentityProviderConfigMap {
    private boolean enableRegistration = true;
    private boolean enableDelete = true;
    private boolean enableUpdate = true;

    private boolean enablePasswordReset = true;
    private boolean enablePasswordSet = true;
    private int passwordResetValidity;

    private boolean confirmationRequired = true;
    private int confirmationValidity;

    // password policy, optional
    private int passwordMinLength;
    private int passwordMaxLength;
    private boolean passwordRequireAlpha;
    private boolean passwordRequireNumber;
    private boolean passwordRequireSpecial;
    private boolean passwordSupportWhitespace;

    public boolean isEnableRegistration() {
        return enableRegistration;
    }

    public void setEnableRegistration(boolean enableRegistration) {
        this.enableRegistration = enableRegistration;
    }

    public boolean isEnableDelete() {
        return enableDelete;
    }

    public void setEnableDelete(boolean enableDelete) {
        this.enableDelete = enableDelete;
    }

    public boolean isEnableUpdate() {
        return enableUpdate;
    }

    public void setEnableUpdate(boolean enableUpdate) {
        this.enableUpdate = enableUpdate;
    }

    public boolean isEnablePasswordReset() {
        return enablePasswordReset;
    }

    public void setEnablePasswordReset(boolean enablePasswordReset) {
        this.enablePasswordReset = enablePasswordReset;
    }

    public boolean isEnablePasswordSet() {
        return enablePasswordSet;
    }

    public void setEnablePasswordSet(boolean enablePasswordSet) {
        this.enablePasswordSet = enablePasswordSet;
    }

    public int getPasswordResetValidity() {
        return passwordResetValidity;
    }

    public void setPasswordResetValidity(int passwordResetValidity) {
        this.passwordResetValidity = passwordResetValidity;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    public int getConfirmationValidity() {
        return confirmationValidity;
    }

    public void setConfirmationValidity(int confirmationValidity) {
        this.confirmationValidity = confirmationValidity;
    }

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

}
