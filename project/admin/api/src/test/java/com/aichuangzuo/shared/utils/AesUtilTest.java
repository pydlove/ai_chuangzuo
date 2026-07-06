package com.aichuangzuo.shared.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AesUtilTest {

    @Test
    void shouldEncryptAndDecrypt() throws Exception {
        String secret = "0123456789abcdef0123456789abcdef";
        String plain = "sk-test-key-12345";

        String encrypted = AesUtil.encrypt(plain, secret);

        assertNotEquals(plain, encrypted);
        assertEquals(plain, AesUtil.decrypt(encrypted, secret));
    }
}
