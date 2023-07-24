package it.smartcommunitylab.aac.webauthn.model;

import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

public class CredentialCreationInfo {

    private ByteArray userHandle;

    // TODO handle serializable, this is NOT serializable by itself
    private PublicKeyCredentialCreationOptions options;

    public ByteArray getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(ByteArray userHandle) {
        this.userHandle = userHandle;
    }

    public PublicKeyCredentialCreationOptions getOptions() {
        return options;
    }

    public void setOptions(PublicKeyCredentialCreationOptions options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return "CredentialCreationInfo [userHandle=" + userHandle + ", options=" + options + "]";
    }
}
