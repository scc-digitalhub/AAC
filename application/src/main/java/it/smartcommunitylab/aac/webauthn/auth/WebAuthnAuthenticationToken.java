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

package it.smartcommunitylab.aac.webauthn.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import java.util.Collection;
import org.springframework.data.util.Pair;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public class WebAuthnAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    private final String userHandle;
    private final transient AssertionRequest assertionRequest;
    private final transient AssertionResult assertionResult;

    private String assertion;
    private InternalUserAccount account;

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

    public WebAuthnAuthenticationToken(
        String userHandle,
        AssertionRequest assertionRequest,
        String assertion,
        AssertionResult assertionResult
    ) {
        super(null);
        this.userHandle = userHandle;
        this.assertionRequest = assertionRequest;
        this.assertion = assertion;
        this.assertionResult = assertionResult;
        setAuthenticated(false);
    }

    public WebAuthnAuthenticationToken(
        String userHandle,
        AssertionRequest assertionRequest,
        String assertion,
        AssertionResult assertionResult,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.userHandle = userHandle;
        this.assertionRequest = assertionRequest;
        this.assertion = assertion;
        this.assertionResult = assertionResult;
        super.setAuthenticated(true);
    }

    public WebAuthnAuthenticationToken(
        String userHandle,
        AssertionRequest assertionRequest,
        String assertion,
        AssertionResult assertionResult,
        InternalUserAccount account,
        Collection<? extends GrantedAuthority> authorities
    ) {
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

    @JsonIgnore
    public InternalUserAccount getAccount() {
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
        // TODO evaluate removal
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
        Assert.isTrue(
            !isAuthenticated,
            "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead"
        );
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
