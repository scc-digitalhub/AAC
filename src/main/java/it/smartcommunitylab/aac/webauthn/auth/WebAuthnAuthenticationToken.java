package it.smartcommunitylab.aac.webauthn.auth;

import java.util.Collection;

import org.springframework.data.util.Pair;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

public class WebAuthnAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    private final String userHandle;
    private final AssertionRequest assertionRequest;
    private final AssertionResult assertionResult;

    private String assertion;
    private WebAuthnUserAccount account;

    public WebAuthnAuthenticationToken(String userHandle, AssertionRequest assertionRequest) {
        super(null);
        this.userHandle = userHandle;
        this.assertionRequest = assertionRequest;
        this.assertionResult = null;
        setAuthenticated(false);
    }

    public WebAuthnAuthenticationToken(String userHandle, AssertionRequest assertionRequest, String assertion) {
        super(null);
        this.userHandle = userHandle;
        this.assertionRequest = assertionRequest;
        this.assertion = assertion;
        this.assertionResult = null;
        setAuthenticated(false);
    }

    public WebAuthnAuthenticationToken(String userHandle, AssertionRequest assertionRequest, String assertion,
            AssertionResult assertionResult) {
        super(null);
        this.userHandle = userHandle;
        this.assertionRequest = assertionRequest;
        this.assertion = assertion;
        this.assertionResult = assertionResult;
        setAuthenticated(false);
    }

    public WebAuthnAuthenticationToken(String userHandle, AssertionRequest assertionRequest, String assertion,
            AssertionResult assertionResult,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userHandle = userHandle;
        this.assertionRequest = assertionRequest;
        this.assertion = assertion;
        this.assertionResult = assertionResult;
        super.setAuthenticated(true);
    }

    public WebAuthnAuthenticationToken(String userHandle, AssertionRequest assertionRequest, String assertion,
            AssertionResult assertionResult, WebAuthnUserAccount account,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userHandle = userHandle;
        this.assertionRequest = assertionRequest;
        this.assertion = assertion;
        this.assertionResult = assertionResult;
        this.account = account;
        super.setAuthenticated(true);
    }

    public String getUserHandle() {
        return userHandle;
    }

    public WebAuthnUserAccount getAccount() {
        return account;
    }

    public AssertionRequest getAssertionRequest() {
        return assertionRequest;
    }

    public String getAssertion() {
        return assertion;
    }

    public AssertionResult getAssertionResult() {
        return assertionResult;
    }

    @Override
    public Object getCredentials() {
        if (assertionRequest != null && assertionResult != null) {
            return Pair.of(assertionRequest, assertionResult);
        } else {
            return null;
        }
    }

    @Override
    public Object getPrincipal() {
        return (this.account == null ? this.userHandle : this.account);
    }

    @Override
    public String getName() {
        return this.userHandle;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        Assert.isTrue(!isAuthenticated,
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        // TODO evaluate removal request/response

        if (this.account != null) {
            this.account.eraseCredentials();
        }

    }
}
