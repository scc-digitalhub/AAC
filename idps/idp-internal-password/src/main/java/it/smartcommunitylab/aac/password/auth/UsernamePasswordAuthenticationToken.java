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

package it.smartcommunitylab.aac.password.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public class UsernamePasswordAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String username;
    private String password;

    private InternalUserAccount account;

    public UsernamePasswordAuthenticationToken(String username, String password) {
        super(null);
        this.username = username;
        this.password = password;
        setAuthenticated(false);
    }

    public UsernamePasswordAuthenticationToken(
        String username,
        String password,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.username = username;
        this.password = password;
        super.setAuthenticated(true);
    }

    public UsernamePasswordAuthenticationToken(
        String username,
        String password,
        InternalUserAccount account,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.username = username;
        this.password = password;
        this.account = account;
        super.setAuthenticated(true);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @JsonIgnore
    public InternalUserAccount getAccount() {
        return account;
    }

    @Override
    public Object getCredentials() {
        return this.password;
    }

    @Override
    public Object getPrincipal() {
        return (this.account == null ? this.username : this.account);
    }

    @Override
    public String getName() {
        return this.username;
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
        this.password = null;
        if (this.account != null) {
            this.account.eraseCredentials();
        }
    }
}
