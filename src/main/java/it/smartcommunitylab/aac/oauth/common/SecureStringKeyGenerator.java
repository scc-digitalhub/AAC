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

package it.smartcommunitylab.aac.oauth.common;

import java.nio.charset.Charset;
import java.util.Base64;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.keygen.StringKeyGenerator;

public class SecureStringKeyGenerator implements StringKeyGenerator {

    private static final int DEFAULT_KEY_LENGTH = 20;
    private static final Charset DEFAULT_ENCODE_CHARSET = Charset.forName("US-ASCII");

    private final Charset charset;
    private final BytesKeyGenerator generator;

    public SecureStringKeyGenerator() {
        this(DEFAULT_KEY_LENGTH);
    }

    public SecureStringKeyGenerator(int keyLength) {
        this(DEFAULT_KEY_LENGTH, DEFAULT_ENCODE_CHARSET);
    }

    public SecureStringKeyGenerator(int keyLength, Charset charset) {
        this.generator = KeyGenerators.secureRandom(keyLength);
        this.charset = charset;
    }

    @Override
    public String generateKey() {
        byte[] key = generator.generateKey();
        byte[] encoded = Base64.getUrlEncoder().withoutPadding().encode(key);
        return new String(encoded, charset);
    }
}
