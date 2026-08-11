package it.smartcommunitylab.aac.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class OAuth2EventListenerUnitTest {

    @Test
    public void testMask_nullOrEmpty() throws Exception {
        // Test with null
        assertNull(OAuth2EventListener.mask(null));

        // Test with an empty string
        assertNull(OAuth2EventListener.mask(""));
    }

    @Test
    public void testMask_singleChar() throws Exception {
        // Test with a single character token
        String token = "x";
        assertEquals("x", OAuth2EventListener.mask(token));
    }

    @Test
    public void testMask_jwtTokenMasksSignatureOnly() throws Exception {
        // Test with a JWT token (header.payload.signature)
        String token = "header.payload.signature123";
        String expected = "header.payload." + "*".repeat("signature123".length());
        assertEquals(expected, OAuth2EventListener.mask(token));
    }

    @Test
    public void testMask_jwtTokenMalformed() throws Exception {
        // JWT missing signature part with empty parts
        // signature missing behave like an opaque token
        String token1 = "header.payload.";
        String expected1 = "header." + "*".repeat(token1.length() - token1.length() / 2);
        assertEquals(expected1, OAuth2EventListener.mask(token1));

        String token2 = "header123.payload.";
        String expected2 = "header12" + "*".repeat(token2.length() - 8);
        assertEquals(expected2, OAuth2EventListener.mask(token2));

        // header or payload missing behave like a normal JWT becouse there are still 3 part when the string is splitted
        String token3 = "header123..signature";
        String expected3 = "header123.." + "*".repeat("signature".length());
        assertEquals(expected3, OAuth2EventListener.mask(token3));

        String token4 = ".payload123.signature";
        String expected4 = ".payload123." + "*".repeat("signature".length());
        assertEquals(expected4, OAuth2EventListener.mask(token4));

        // JWT with parts > 3
        // signature missing behave like an opaque token becouse it fails the parts.length == 3 check
        String token5 = "header.payload.signature.more";
        String expected5 = "header.p" + "*".repeat(token5.length() - 8);
        assertEquals(expected5, OAuth2EventListener.mask(token5));
    }

    @Test
    public void testMask_opaqueTokenMasksMoreThan8Visible() throws Exception {
        // Test with a token of length 20, expecting 8 visible characters and 12 masked characters
        String token1 = "12345678901234567890";
        String expected1 = "12345678************";
        assertEquals(expected1, OAuth2EventListener.mask(token1));

        // Test with a token of length 16, expecting 8 visible characters and 8 masked characters
        String token2 = "1234567890123456";
        String expected2 = "12345678********";
        assertEquals(expected2, OAuth2EventListener.mask(token2));
    }

    @Test
    public void testMask_opaqueTokenMasksHalfLengthVisible() throws Exception {
        // Test with a token of length 10, expecting 5 visible characters and 5 masked characters
        String token1 = "1234567890";
        String expected1 = "12345*****";
        assertEquals(expected1, OAuth2EventListener.mask(token1));

        // Test with a token of length 7, expecting 3 visible characters and 4 masked characters
        String token2 = "1234567";
        String expected2 = "123****";
        assertEquals(expected2, OAuth2EventListener.mask(token2));

        // Test with a token of length 2, expecting 1 visible character and 1 masked character
        String token3 = "12";
        String expected3 = "1*";
        assertEquals(expected3, OAuth2EventListener.mask(token3));
    }
}
