package com.aichuangzuo.admin.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KimiProviderClientTest {

    private final KimiProviderClient client = new KimiProviderClient();

    @Test
    void trim_shouldRemoveTrailingV1() {
        assertEquals("https://api.moonshot.cn",
                invokeTrim("https://api.moonshot.cn/v1"));
        assertEquals("https://api.moonshot.cn",
                invokeTrim("https://api.moonshot.cn/v1/"));
    }

    @Test
    void trim_shouldPreserveProxyPath() {
        assertEquals("https://api.kimi.com/coding",
                invokeTrim("https://api.kimi.com/coding/"));
        assertEquals("https://api.kimi.com/coding",
                invokeTrim("https://api.kimi.com/coding"));
        assertEquals("https://api.kimi.com/coding",
                invokeTrim("https://api.kimi.com/coding/v1"));
        assertEquals("https://api.kimi.com/coding",
                invokeTrim("https://api.kimi.com/coding/v1/"));
    }

    @Test
    void trim_shouldHandleCleanBaseUrl() {
        assertEquals("https://api.moonshot.cn",
                invokeTrim("https://api.moonshot.cn"));
        assertEquals("https://api.moonshot.cn",
                invokeTrim("https://api.moonshot.cn/"));
    }

    @Test
    void trim_shouldHandleNull() {
        assertEquals("", invokeTrim(null));
    }

    private String invokeTrim(String baseUrl) {
        return (String) ReflectionTestUtils.invokeMethod(client, "trim", baseUrl);
    }
}
