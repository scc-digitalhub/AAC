/**
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

package it.smartcommunitylab.aac.jwt;

import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWEKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.Key;
import java.security.PrivateKey;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import net.jcip.annotations.ThreadSafe;
import org.springframework.util.Assert;

@ThreadSafe
public class JWEPublicKeySelector<C extends SecurityContext> implements JWEKeySelector<C> {

    private final JWKSource<C> jwkSource;

    public JWEPublicKeySelector(JWKSource<C> jwkSource) {
        Assert.notNull(jwkSource, "jwk source is required");

        this.jwkSource = jwkSource;
    }

    @Override
    public List<? extends Key> selectJWEKeys(JWEHeader header, C context) throws KeySourceException {
        //match by header
        JWKMatcher jwkMatcher = JWKMatcher.forJWEHeader(header);
        List<JWK> jwkMatches = jwkSource.get(new JWKSelector(jwkMatcher), context);

        //keep only public keys
        return KeyConverter.toJavaKeys(jwkMatches).stream().filter(this::isPublicKey).collect(Collectors.toList());
    }

    private boolean isPublicKey(Key key) {
        //basic check
        //TODO refactor
        return !(key instanceof PrivateKey || key instanceof SecretKey);
    }
}
