package ru.mts.confluence.settings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class TokenCipherTest {
    private final TokenCipher cipher = new TokenCipher(TokenCipher.generateKey());

    @Test
    public void roundTrip() {
        assertEquals("glpat-секрет", cipher.decrypt(cipher.encrypt("glpat-секрет")));
    }

    @Test
    public void usesRandomNonce() {
        assertNotEquals(cipher.encrypt("t"), cipher.encrypt("t"));
    }

    @Test
    public void doesNotLeakPlaintext() {
        assertFalse(cipher.encrypt("glpat-abc").contains("glpat-abc"));
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsTamperedData() {
        String enc = cipher.encrypt("token");
        String tampered = enc.substring(0, enc.length() - 4) + (enc.endsWith("AAAA") ? "BBBB" : "AAAA");
        cipher.decrypt(tampered);
    }
}
