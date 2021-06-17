package it.smartcommunitylab.aac.oauth.store;

import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;

public class HumanStringKeyGenerator implements StringKeyGenerator {

    private static final int DEFAULT_KEY_LENGTH = 6;
    private final RandomValueStringGenerator generator;

    public HumanStringKeyGenerator() {
        this(DEFAULT_KEY_LENGTH);

    }

    public HumanStringKeyGenerator(int keyLength) {
        generator = new RandomValueStringGenerator(keyLength);
    }

    @Override
    public String generateKey() {
        String key = generator.generate();

        return key;
    }

}
