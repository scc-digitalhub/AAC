package it.smartcommunitylab.aac.crypto;

import org.springframework.security.crypto.password.PasswordEncoder;

public class PlaintextPasswordEncoder implements PasswordEncoder {

    public String encode(CharSequence rawPassword) {
        return rawPassword.toString();
    }

    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return rawPassword.toString().equals(encodedPassword);
    }

    /*
     * Access only via static instance
     */

    public static PasswordEncoder getInstance() {
        return INSTANCE;
    }

    private static final PasswordEncoder INSTANCE = new PlaintextPasswordEncoder();

    private PlaintextPasswordEncoder() {
    }

}