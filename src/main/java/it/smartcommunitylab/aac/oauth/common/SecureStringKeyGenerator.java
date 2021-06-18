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
        byte[] encoded = Base64.getUrlEncoder().encode(key);
        return new String(encoded, charset);
    }

}
