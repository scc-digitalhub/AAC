package it.smartcommunitylab.aac.test.openid;

public class HybridFlowTest {

    public void hybridFlowWithToken() {
        // wrong, return access_token AND id_token AND no code
        // should return access_token + code
    }

    public void hybridFlowWithIdToken() {
        // not supported, returns only code
        // should return code + id_token

    }

    public void hybridFlowWithTokenIdToken() {
        // wrong, return access_token AND id_token AND no code
        // should return access_token + id_token + code
    }
}
