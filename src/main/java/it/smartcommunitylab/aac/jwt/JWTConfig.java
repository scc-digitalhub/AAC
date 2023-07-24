package it.smartcommunitylab.aac.jwt;

import it.smartcommunitylab.aac.oauth.model.EncryptionMethod;
import it.smartcommunitylab.aac.oauth.model.JWEAlgorithm;
import it.smartcommunitylab.aac.oauth.model.JWSAlgorithm;

public class JWTConfig {

    private JWSAlgorithm signAlgorithm;
    private JWEAlgorithm encAlgorithm;
    private EncryptionMethod encMethod;

    public JWSAlgorithm getSignAlgorithm() {
        return signAlgorithm;
    }

    public void setSignAlgorithm(JWSAlgorithm signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }

    public JWEAlgorithm getEncAlgorithm() {
        return encAlgorithm;
    }

    public void setEncAlgorithm(JWEAlgorithm encAlgorithm) {
        this.encAlgorithm = encAlgorithm;
    }

    public EncryptionMethod getEncMethod() {
        return encMethod;
    }

    public void setEncMethod(EncryptionMethod encMethod) {
        this.encMethod = encMethod;
    }
}
